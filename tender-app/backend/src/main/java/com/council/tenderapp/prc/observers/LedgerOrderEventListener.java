package com.council.tenderapp.prc.observers;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;
import com.council.tenderapp.prc.service.LedgerService;

@Component
public class LedgerOrderEventListener implements OrderObserver {

    private static final Logger logger = LoggerFactory.getLogger(LedgerOrderEventListener.class);
    private final LedgerService ledgerService;

    public LedgerOrderEventListener(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    public void onOrderStatusChanged(OrderStatusEvent event) {
        OrderStatus currentStatus = event.newStatus();

        try {
            ledgerService.createLedgerEntry(event.orderId(), currentStatus.toString());
        } catch (Exception exception) {
            logger.error("Failed to create ledger entry for order ID: {}", event.orderId(), exception);
        }
    }

}
