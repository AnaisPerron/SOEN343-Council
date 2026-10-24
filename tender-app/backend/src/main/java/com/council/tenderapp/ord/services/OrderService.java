package com.council.tenderapp.ord.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.dtos.OrderItemRequestDto;
import com.council.tenderapp.ord.dtos.OrderLineDto;
import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.ord.exceptions.MenuItemUnavailableException;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.OrderLineRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.technicalservices.payment.PaymentDeclinedException;
import com.council.tenderapp.technicalservices.payment.PaymentGateway;
import com.council.tenderapp.technicalservices.payment.PaymentResult;

@Service
public class OrderService {

    private final BrowseService browseService;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final PaymentGateway paymentGateway;
    private final PreparationQueueService preparationQueueService;

    public OrderService(BrowseService browseService, MenuItemRepository menuItemRepository, OrderRepository orderRepository,
            OrderLineRepository orderLineRepository, PaymentGateway paymentGateway, PreparationQueueService preparationQueueService) {
        this.browseService = browseService;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.paymentGateway = paymentGateway;
        this.preparationQueueService = preparationQueueService;
    }

    @Transactional
    public OrderResponseDto placeOrder(Long customerId, PlaceOrderRequestDto request) {
        browseService.checkRestaurantOpen(request.restaurantId());

        // Only this restaurant's items are looked up, so an item from any other restaurant is rejected
        Map<Long, MenuItem> menu = menuItemRepository.findMenuItemsByRestaurantId(request.restaurantId())
                .stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        for (OrderItemRequestDto item : request.items()) {
            MenuItem menuItem = menu.get(item.menuItemId());
            if (menuItem == null) {
                throw new InvalidOrderException("Menu item " + item.menuItemId() + " does not belong to restaurant " + request.restaurantId());
            }
            if (!"available".equalsIgnoreCase(menuItem.getStatus())) {
                throw new MenuItemUnavailableException("Menu item " + menuItem.getId() + " : " + menuItem.getName() + " is not available");
            }
        }

        BigDecimal total = request.items().stream()
                .map(item -> menu.get(item.menuItemId()).getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PaymentResult payment = paymentGateway.charge(customerId, total);
        if (!payment.approved()) {
            throw new PaymentDeclinedException("Payment declined: " + payment.message());
        }

        Order order = orderRepository.save(new Order(customerId, request.restaurantId(), Order.STATUS_PLACED, Instant.now()));

        List<OrderLine> lines = orderLineRepository.saveAll(request.items().stream()
                .map(item -> new OrderLine(order.getId(), item.menuItemId(), item.quantity(), menu.get(item.menuItemId()).getPrice()))
                .toList());

        preparationQueueService.enqueue(order);

        List<OrderLineDto> lineDtos = lines.stream()
                .map(line -> new OrderLineDto(
                    line.getMenuItemId(),
                    menu.get(line.getMenuItemId()).getName(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getLineTotal()
                ))
                .toList();

        return new OrderResponseDto(order.getId(), order.getRestaurantId(), order.getStatus(), total,
                order.getCreatedAt(), payment.reference(), lineDtos);
    }
}
