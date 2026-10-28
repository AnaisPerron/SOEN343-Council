package com.council.tenderapp.ord.observers;

import com.council.tenderapp.ord.events.OrderStatusEvent;

public interface OrderObserver {
    void onOrderStatusChanged(OrderStatusEvent event);
}
