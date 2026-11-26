package com.council.tenderapp.ord.services;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.council.tenderapp.ord.dtos.OrderItemRequestDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.MenuItemOption;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.ord.exceptions.MenuItemUnavailableException;
import com.council.tenderapp.ord.repositories.MenuItemOptionRepository;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.prc.service.PricingService;
import com.council.tenderapp.reg.services.CustomerProfileService;

/**
 * Validates a checkout request and prices it, before any payment or persistence.
 * Extracted from {@link CustomerOrderService} so checkout keeps only payment and persistence.
 */
class OrderPreparation {
    private final BrowseService browseService;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;
    private final CustomerProfileService customerProfileService;
    private final PricingService pricingService;

    OrderPreparation(BrowseService browseService, MenuItemRepository menuItemRepository,
            MenuItemOptionRepository menuItemOptionRepository, CustomerProfileService customerProfileService,
            PricingService pricingService) {
        this.browseService = browseService;
        this.menuItemRepository = menuItemRepository;
        this.menuItemOptionRepository = menuItemOptionRepository;
        this.customerProfileService = customerProfileService;
        this.pricingService = pricingService;
    }

    record PreparedOrder(Map<Long, MenuItem> menu, Map<Long, MenuItemOption> options,
            String deliveryAddress, Double latitude, Double longitude, PricingService.Quote quote) {}

    private record Delivery(String address, Double latitude, Double longitude) {}

    PreparedOrder prepare(Long customerId, PlaceOrderRequestDto request) {
        boolean override = request.deliveryAddress() != null;
        validateDeliveryOverride(request, override);
        browseService.checkRestaurantOpen(request.restaurantId());
        Map<Long, MenuItem> menu = availableMenu(request);
        Map<Long, MenuItemOption> options = selectedOptions(request);
        BigDecimal subtotal = request.items().stream()
                .map(item -> paidUnitPrice(menu.get(item.menuItemId()), item, options)
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // The saved address is always read, as before the extraction, even when the request overrides it.
        Delivery saved = savedDelivery(customerId);
        Delivery delivery = override
                ? new Delivery(request.deliveryAddress().trim(), request.deliveryLatitude(), request.deliveryLongitude())
                : saved;
        if (delivery.address() == null || delivery.address().isBlank()) {
            throw new InvalidOrderException("A verified delivery address is required");
        }
        PricingService.Quote quote = pricingService.quote(subtotal, request.restaurantId(),
                delivery.latitude(), delivery.longitude());
        return new PreparedOrder(menu, options, delivery.address(), delivery.latitude(), delivery.longitude(), quote);
    }

    private static void validateDeliveryOverride(PlaceOrderRequestDto request, boolean override) {
        if (override && (request.deliveryAddress().isBlank() || request.deliveryLatitude() == null || request.deliveryLongitude() == null)) {
            throw new InvalidOrderException("Pick a verified delivery address from the suggestions");
        }
        if (!override && (request.deliveryLatitude() != null || request.deliveryLongitude() != null)) {
            throw new InvalidOrderException("A delivery location requires an address");
        }
    }

    private Map<Long, MenuItem> availableMenu(PlaceOrderRequestDto request) {
        Map<Long, MenuItem> menu = menuItemRepository.findMenuItemsByRestaurantId(request.restaurantId())
                .stream().collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        for (OrderItemRequestDto item : request.items()) {
            MenuItem menuItem = menu.get(item.menuItemId());
            if (menuItem == null) {
                throw new InvalidOrderException("Menu item " + item.menuItemId()
                        + " does not belong to restaurant " + request.restaurantId());
            }
            if (!"available".equalsIgnoreCase(menuItem.getStatus())) {
                throw new MenuItemUnavailableException("Menu item " + menuItem.getId()
                        + " : " + menuItem.getName() + " is not available");
            }
        }
        return menu;
    }

    private Map<Long, MenuItemOption> selectedOptions(PlaceOrderRequestDto request) {
        Map<Long, MenuItemOption> options = menuItemOptionRepository
                .findByMenuItemIdIn(request.items().stream().map(OrderItemRequestDto::menuItemId).distinct().toList())
                .stream().collect(Collectors.toMap(MenuItemOption::getId, Function.identity()));
        for (OrderItemRequestDto item : request.items()) {
            for (Long optionId : item.selectedOptionIds()) {
                MenuItemOption option = options.get(optionId);
                if (option == null || !item.menuItemId().equals(option.getMenuItemId())) {
                    throw new InvalidOrderException("Option " + optionId
                            + " does not belong to menu item " + item.menuItemId());
                }
            }
        }
        return options;
    }

    private Delivery savedDelivery(Long customerId) {
        return customerProfileService.deliveryAddress(customerId)
                .map(saved -> new Delivery(saved.address(), saved.latitude(), saved.longitude()))
                .orElse(new Delivery(null, null, null));
    }

    // What the customer pays per unit: the item's price plus its selected add-ons.
    static BigDecimal paidUnitPrice(MenuItem menuItem, OrderItemRequestDto item, Map<Long, MenuItemOption> options) {
        return item.selectedOptionIds().stream()
                .map(optionId -> options.get(optionId).getAdditionalCost())
                .reduce(menuItem.getPrice(), BigDecimal::add);
    }
}
