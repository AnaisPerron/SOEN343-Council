package com.council.tenderapp.technicalservices.payment;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tender-cash")
@PreAuthorize("hasRole('CUSTOMER')")
public class TenderCashController {
    private final TenderCash cash;
    public TenderCashController(TenderCash cash) { this.cash = cash; }
    @GetMapping
    public TenderCash.Balance balance(@AuthenticationPrincipal Long customerId) { return cash.balance(customerId); }
}
