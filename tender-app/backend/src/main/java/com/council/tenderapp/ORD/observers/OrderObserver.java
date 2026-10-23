package com.council.tenderapp.ORD.observers;

import com.council.tenderapp.ORD.events.OrderStatusEvent;

public interface OrderObserver {
    void onOrderStatusChanged(OrderStatusEvent event);
}
