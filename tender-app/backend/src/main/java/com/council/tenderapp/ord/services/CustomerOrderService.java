package com.council.tenderapp.ord.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import com.council.tenderapp.ord.repositories.MenuItemOptionRepository;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.services.OrderPreparation.PreparedOrder;
import com.council.tenderapp.reg.services.CustomerProfileService;
import com.council.tenderapp.prc.service.PricingService;
import com.council.tenderapp.prc.service.LedgerService;
import com.council.tenderapp.technicalservices.payment.PaymentDeclinedException;
import com.council.tenderapp.technicalservices.payment.MockCardPayment;
import com.council.tenderapp.technicalservices.payment.PaymentResult;
import com.council.tenderapp.technicalservices.payment.PaymentStrategy;
import com.council.tenderapp.technicalservices.payment.TenderCash;
import com.council.tenderapp.technicalservices.payment.TenderCashPayment;

@Service
public class CustomerOrderService {
    private final OrderRepository orderRepository;
    private final PreparationQueueService preparationQueueService;
    private final OrderCancellationService cancellations;
    private final JdbcTemplate jdbc;
    private final CustomerProfileService customerProfileService;
    private final PricingService pricingService;
    private final OrderPreparation preparation;
    private final LedgerService ledgerService;
    private final TenderCash tenderCash;
    // Strategies for direct checkout; PayPal checkout passes its own PayPalPayment.
    private final PaymentStrategy tenderCashPayment;
    private final PaymentStrategy cardPayment;

    public CustomerOrderService(BrowseService browseService, MenuItemRepository menuItemRepository,
            MenuItemOptionRepository menuItemOptionRepository, OrderRepository orderRepository,
            PreparationQueueService preparationQueueService, JdbcTemplate jdbc,
            CustomerProfileService customerProfileService, OrderCancellationService cancellations,
            PricingService pricingService, LedgerService ledgerService, TenderCash tenderCash,
            TenderCashPayment tenderCashPayment, MockCardPayment cardPayment) {
        this.orderRepository = orderRepository;
        this.preparationQueueService = preparationQueueService;
        this.cancellations = cancellations;
        this.jdbc = jdbc;
        this.customerProfileService = customerProfileService;
        this.pricingService = pricingService;
        this.ledgerService = ledgerService;
        this.tenderCash = tenderCash;
        this.tenderCashPayment = tenderCashPayment;
        this.cardPayment = cardPayment;
        this.preparation = new OrderPreparation(browseService, menuItemRepository, menuItemOptionRepository,
                customerProfileService, pricingService);
    }

    /** Direct checkout: Tender Cash when it covers the whole order, otherwise the card strategy. */
    @Transactional
    public OrderResponseDto placeOrder(Long customerId, PlaceOrderRequestDto request) {
        return checkout(customerId, request, amountDue -> amountDue.signum() == 0 ? tenderCashPayment : cardPayment);
    }

    /** Checkout with a payment strategy chosen by the caller, e.g. a verified PayPal capture. */
    @Transactional
    public OrderResponseDto placeOrder(Long customerId, PlaceOrderRequestDto request, PaymentStrategy payment) {
        return checkout(customerId, request, amountDue -> payment);
    }

    private OrderResponseDto checkout(Long customerId, PlaceOrderRequestDto request,
            Function<BigDecimal, PaymentStrategy> strategyFor) {
        PreparedOrder prepared = preparation.prepare(customerId, request);
        tenderCash.lockCustomer(customerId);
        BigDecimal amountDue = prepared.quote().total().subtract(request.tenderAmount() == 0 ? BigDecimal.ZERO
                : tenderCash.discount(customerId, request.tenderAmount(), prepared.quote().total()));
        PaymentStrategy strategy = strategyFor.apply(amountDue);
        PaymentResult payment = strategy.pay(customerId, amountDue);
        if (!payment.approved()) {
            throw new PaymentDeclinedException("Payment declined: " + payment.message());
        }
        return persistOrder(customerId, request, prepared, payment, amountDue);
    }

    private OrderResponseDto persistOrder(Long customerId, PlaceOrderRequestDto request,
            PreparedOrder prepared, PaymentResult payment, BigDecimal paid) {
        Map<Long, MenuItem> menu = prepared.menu();
        Map<Long, MenuItemOption> options = prepared.options();
        PricingService.Quote quote = prepared.quote();
        BigDecimal total = quote.total();
        customerProfileService.ensureExists(customerId);
        Instant placedAt = Instant.now();
        Order order = new Order(customerId, request.restaurantId(), OrderStatus.PLACED, total);
        order.setDelivery(prepared.deliveryAddress(), prepared.latitude(), prepared.longitude());
        for (OrderItemRequestDto item : request.items()) {
            order.addOrderLine(new OrderLine(menu.get(item.menuItemId()), item.quantity(),
                    menu.get(item.menuItemId()).getPrice()));
        }
        order = orderRepository.save(order);
        tenderCash.placed(order, request.tenderAmount(), paid);
        pricingService.saveBill(order, quote);
        ledgerService.createLedgerEntry(order.getId(), OrderStatus.PLACED.name());
        // Lines keep the base price; each add-on's cost is snapshotted in order_line_options.
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
            BigDecimal unitPrice = OrderPreparation.paidUnitPrice(line.getMenuItem(), request.items().get(i), options);
            lineDtos.add(new OrderLineDto(line.getMenuItemId(), line.getMenuItem().getName(),
                    line.getQuantity(), unitPrice, unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()))));
        }
        return new OrderResponseDto(order.getId(), order.getRestaurantId(),
                order.getStatus().name().toLowerCase(), order.getTotalPrice(), placedAt,
                payment.reference(), lineDtos);
    }

    @Transactional(readOnly = true)
    public PricingService.Quote quoteOrder(Long customerId, PlaceOrderRequestDto request) {
        return preparation.prepare(customerId, request).quote();
    }

    @Transactional
    public OrderStatusResponseDto cancelOrder(Long customerId, Long orderId) {
        return cancellations.cancelForCustomer(customerId, orderId);
    }
}
