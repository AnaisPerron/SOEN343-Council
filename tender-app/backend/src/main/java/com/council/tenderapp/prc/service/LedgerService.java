package com.council.tenderapp.prc.service;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public LedgerEntry createLedgerEntry(Long orderId, String orderStatus)
    {
        Double totalCharge = billService.calculateTotalCharge(orderId);
        Long billId = billService.getBillIdByOrderId(orderId);
        String transaction_type = getTransactionType(orderStatus);

        // The payment record follows the order through its normal state changes.
        // A refund is a second transaction so the original charge remains auditable.
        var latest = ledgerEntryRepository.findByOrderId(orderId);
        if ("REFUNDED".equals(orderStatus) && latest.isEmpty()) {
            LedgerEntry payment = new LedgerEntry();
            payment.setBillId(billId);
            payment.setTransactionType("Payment");
            payment.setFinalOrderStatus("DELIVERED");
            payment.setTotalCharge(totalCharge);
            ledgerEntryRepository.save(payment);
        }
        if ("REFUNDED".equals(orderStatus) && latest.isPresent()
                && "Refund".equals(latest.get().getTransactionType())) {
            return latest.get();
        }
        LedgerEntry ledgerEntry = "REFUNDED".equals(orderStatus) ? new LedgerEntry()
                : latest
                    .filter(entry -> !"Refund".equals(entry.getTransactionType()))
                    .orElseGet(LedgerEntry::new);
        ledgerEntry.setBillId(billId);
        ledgerEntry.setTransactionType(transaction_type);
        ledgerEntry.setFinalOrderStatus(orderStatus);
        ledgerEntry.setTotalCharge(totalCharge);
        ledgerEntry.setTimestamp(OffsetDateTime.now());

        return ledgerEntryRepository.save(ledgerEntry);
    }

    private String getTransactionType(String orderStatus) 
    {
        return switch (orderStatus) {
            case "PLACED", "ACCEPTED", "PREPARING", "READY", "PICKED_UP", "DELIVERED", "CANCELLED" -> "Payment";
            case "REJECTED", "REFUNDED" -> "Refund";
            default -> throw new IllegalArgumentException("Invalid order status: " + orderStatus);
        };
    }
}
