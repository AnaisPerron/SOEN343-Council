package com.council.tenderapp.ord.observers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.council.tenderapp.ord.events.OrderStatusEvent;

@Component
public class OrderRecord implements OrderObserver {

    private static final Logger logger = LoggerFactory.getLogger(OrderRecord.class);

    @Override
    public void onOrderStatusChanged(OrderStatusEvent event) {
        String reasonDetails = event.rejectionReason() == null
            ? ""
            : " | Rejection reason: \"" + event.rejectionReason()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ") + "\"";
        logger.info(
            "{}: Restaurant_{} {} order #{}: new status: {}{}",
            event.eventId(),
            event.restaurantId(),
            event.action(),
            event.orderId(),
            event.newStatus(),
            reasonDetails
        );
    }
}
