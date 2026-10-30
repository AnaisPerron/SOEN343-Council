package com.council.tenderapp.ord.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.dtos.OrderItemRequestDto;
import com.council.tenderapp.ord.dtos.OrderLineDto;
import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.MenuItemOption;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.ord.exceptions.MenuItemUnavailableException;
import com.council.tenderapp.ord.repositories.MenuItemOptionRepository;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.reg.services.CustomerProfileService;
import com.council.tenderapp.technicalservices.payment.PaymentDeclinedException;
import com.council.tenderapp.technicalservices.payment.PaymentGateway;
import com.council.tenderapp.technicalservices.payment.PaymentResult;

@Service
public class CustomerOrderService {
    private final BrowseService browseService;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PreparationQueueService preparationQueueService;
    private final OrderCancellationService cancellations;
    private final JdbcTemplate jdbc;
    private final CustomerProfileService customerProfileService;

    public CustomerOrderService(BrowseService browseService, MenuItemRepository menuItemRepository,
            MenuItemOptionRepository menuItemOptionRepository, OrderRepository orderRepository,
            PaymentGateway paymentGateway, PreparationQueueService preparationQueueService, JdbcTemplate jdbc,
            CustomerProfileService customerProfileService, OrderCancellationService cancellations) {
        this.browseService = browseService;
        this.menuItemRepository = menuItemRepository;
        this.menuItemOptionRepository = menuItemOptionRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.preparationQueueService = preparationQueueService;
        this.cancellations = cancellations;
        this.jdbc = jdbc;
        this.customerProfileService = customerProfileService;
    }

    @Transactional
    public OrderResponseDto placeOrder(Long customerId, PlaceOrderRequestDto request) {
        browseService.checkRestaurantOpen(request.restaurantId());
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

        BigDecimal total = request.items().stream()
                .map(item -> paidUnitPrice(menu.get(item.menuItemId()), item, options)
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentResult payment = paymentGateway.charge(customerId, total);
        if (!payment.approved()) {
            throw new PaymentDeclinedException("Payment declined: " + payment.message());
        }

        // orders.customer_id references customer_profiles; older accounts may lack that row.
        customerProfileService.ensureExists(customerId);
        Instant placedAt = Instant.now();
        Order order = new Order(customerId, request.restaurantId(), OrderStatus.PLACED, total);
        for (OrderItemRequestDto item : request.items()) {
            order.addOrderLine(new OrderLine(menu.get(item.menuItemId()), item.quantity(),
                    menu.get(item.menuItemId()).getPrice()));
        }
        order = orderRepository.save(order);
        // Lines keep the base price; each add-on's cost is snapshotted in order_line_options.
        // Written with JDBC because the table's composite keys have no JPA entity, so flush first:
        // cascaded lines are otherwise only inserted at commit and would have no id yet.
        orderRepository.flush();
        List<OrderLine> savedLines = order.getOrderLines();
        for (int i = 0; i < savedLines.size(); i++) {
            OrderLine line = savedLines.get(i);
            for (Long optionId : request.items().get(i).selectedOptionIds()) {
                jdbc.update("INSERT INTO order_line_options (order_line_id, menu_item_id, menu_item_option_id, additional_cost) VALUES (?, ?, ?, ?)",
                        line.getId(), line.getMenuItemId(), optionId, options.get(optionId).getAdditionalCost());
            }
        }
        preparationQueueService.enqueue(order, placedAt);

        List<OrderLineDto> lineDtos = new ArrayList<>();
        for (int i = 0; i < savedLines.size(); i++) {
            OrderLine line = savedLines.get(i);
            BigDecimal unitPrice = paidUnitPrice(line.getMenuItem(), request.items().get(i), options);
            lineDtos.add(new OrderLineDto(line.getMenuItemId(), line.getMenuItem().getName(),
                    line.getQuantity(), unitPrice, unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()))));
        }
        return new OrderResponseDto(order.getId(), order.getRestaurantId(),
                order.getStatus().name().toLowerCase(), order.getTotalPrice(), placedAt,
                payment.reference(), lineDtos);
    }

    // What the customer pays per unit: the item's price plus its selected add-ons.
    private static BigDecimal paidUnitPrice(MenuItem menuItem, OrderItemRequestDto item, Map<Long, MenuItemOption> options) {
        return item.selectedOptionIds().stream()
                .map(optionId -> options.get(optionId).getAdditionalCost())
                .reduce(menuItem.getPrice(), BigDecimal::add);
    }

    @Transactional
    public OrderStatusResponseDto cancelOrder(Long customerId, Long orderId) {
        return cancellations.cancelForCustomer(customerId, orderId);
    }
}
