package com.council.tenderapp.technicalservices.payment;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;

@RestController
@RequestMapping("/api/paypal")
@PreAuthorize("hasRole('CUSTOMER')")
public class PayPalCheckoutController {
    public record Config(boolean enabled, String clientId) {}
    public record CreateRequest(@NotEmpty List<@Valid PlaceOrderRequestDto> orders, String returnUrl) {}
    public record CreateResponse(String orderId, String approvalUrl) {}

    private final PayPalCheckoutService paypal;

    public PayPalCheckoutController(PayPalCheckoutService paypal) { this.paypal = paypal; }

    @GetMapping("/config")
    public Config config() { return new Config(paypal.enabled(), paypal.clientId()); }

    @PostMapping("/orders")
    public CreateResponse create(@AuthenticationPrincipal Long customerId, @Valid @RequestBody CreateRequest request) {
        var created = paypal.create(customerId, request.orders(), request.returnUrl());
        return new CreateResponse(created.orderId(), created.approvalUrl());
    }

    @PostMapping("/orders/{paypalOrderId}/capture")
    public List<OrderResponseDto> capture(@AuthenticationPrincipal Long customerId, @PathVariable String paypalOrderId) {
        return paypal.capture(customerId, paypalOrderId);
    }
}
