package com.council.tenderapp.prc.observers;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.prc.service.LedgerService;

@Component
public class LedgerOrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(LedgerOrderEventListener.class);
    private final LedgerService ledgerService;

    public LedgerOrderEventListener(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    // This method listens to 'orderStatusPublisher' events
    @EventListener
    public void onOrderStatusChanged(OrderStatusEvent event) {
        OrderStatus currentStatus = event.newStatus();

        try {
            ledgerService.createLedgerEntry(event.orderId(), currentStatus.toString());
        } catch (Exception exception) {
            logger.error("Failed to create ledger entry for order ID: {}", event.orderId(), exception);
        }
    }

}
