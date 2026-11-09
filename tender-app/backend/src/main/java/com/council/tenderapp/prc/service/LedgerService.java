package com.council.tenderapp.prc.service;

import org.springframework.stereotype.Service;

import com.council.tenderapp.prc.entities.LedgerEntry;
import com.council.tenderapp.prc.repositories.LedgerEntryRepository;

@Service
public class LedgerService 
{
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BillService billService;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, BillService billService) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.billService = billService;
    }

    public LedgerEntry createLedgerEntry(Long orderId, String orderStatus) 
    {
        Double totalCharge = billService.calculateTotalCharge(orderId);
        Long billId = billService.getBillIdByOrderId(orderId);
        String transaction_type = getTransactionType(orderStatus);

        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setBillId(billId);
        ledgerEntry.setTransactionType(transaction_type);
        ledgerEntry.setFinalOrderStatus(orderStatus);
        ledgerEntry.setTotalCharge(totalCharge);

        return ledgerEntryRepository.save(ledgerEntry);
    }

    private String getTransactionType(String orderStatus) 
    {
        return switch (orderStatus) {
            case "DELIVERED", "CANCELLED" -> "Payment";
            case "REJECTED", "REFUNDED" -> "Refund";
            default -> throw new IllegalArgumentException("Invalid order status: " + orderStatus);
        };
    }
}
