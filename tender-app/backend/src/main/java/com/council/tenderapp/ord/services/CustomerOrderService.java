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
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.MenuItemUnavailableException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.technicalservices.payment.PaymentDeclinedException;
import com.council.tenderapp.technicalservices.payment.PaymentGateway;
import com.council.tenderapp.technicalservices.payment.PaymentResult;
import com.council.tenderapp.ord.dto.response.OrderResponse;
import com.council.tenderapp.ord.dto.response.OrderLineResponse;

@Service
public class CustomerOrderService {
    private final BrowseService browseService;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PreparationQueueService preparationQueueService;

    public CustomerOrderService(BrowseService browseService, MenuItemRepository menuItemRepository,
            OrderRepository orderRepository, PaymentGateway paymentGateway,
            PreparationQueueService preparationQueueService) {
        this.browseService = browseService;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.preparationQueueService = preparationQueueService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getHistory(Long customerId) {
        return orderRepository.findByCustomerIdOrderByDateOrderedDescIdDesc(customerId).stream()
            .map(order -> new OrderResponse(order.getId(), order.getRestaurantId(), order.getStatus(),
                order.getDateOrdered(), order.getAcceptedAt(),
                order.getRejectionReason(), order.getTotalPrice(), order.getOrderLines().stream()
                    .map(line -> new OrderLineResponse(line.getMenuItem().getName(), line.getQuantity(), line.getUnitPrice()))
                    .toList()))
            .toList();
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

        BigDecimal total = request.items().stream()
                .map(item -> menu.get(item.menuItemId()).getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentResult payment = paymentGateway.charge(customerId, total);
        if (!payment.approved()) {
            throw new PaymentDeclinedException("Payment declined: " + payment.message());
        }

        Instant placedAt = Instant.now();
        Order order = new Order(customerId, request.restaurantId(), OrderStatus.PLACED, total);
        for (OrderItemRequestDto item : request.items()) {
            order.addOrderLine(new OrderLine(menu.get(item.menuItemId()), item.quantity(),
                    menu.get(item.menuItemId()).getPrice()));
        }
        order = orderRepository.save(order);
        preparationQueueService.enqueue(order, placedAt);

        List<OrderLineDto> lineDtos = order.getOrderLines().stream()
                .map(line -> new OrderLineDto(line.getMenuItemId(), line.getMenuItem().getName(),
                        line.getQuantity(), line.getUnitPrice(),
                        line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))))
                .toList();
        return new OrderResponseDto(order.getId(), order.getRestaurantId(),
                order.getStatus().name().toLowerCase(), order.getTotalPrice(), placedAt,
                payment.reference(), lineDtos);
    }

    @Transactional
    public OrderStatusResponseDto cancelOrder(Long customerId, Long orderId) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        if (!customerId.equals(order.getCustomerId())) {
            throw new OrderAccessDeniedException("Order " + orderId + " does not belong to this customer");
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new InvalidOrderStateException("Order " + orderId
                    + " cannot be cancelled because it is " + order.getStatus().name().toLowerCase());
        }

        String previousStatus = order.getStatus().name().toLowerCase();
        Instant cancelledAt = Instant.now();
        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.save(order);
        preparationQueueService.remove(order, previousStatus, cancelledAt);
        return new OrderStatusResponseDto(order.getId(), previousStatus,
                order.getStatus().name().toLowerCase(), cancelledAt);
    }
}
