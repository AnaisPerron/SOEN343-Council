package com.council.tenderapp.dlv.services;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.council.tenderapp.dlv.domain.NearestIdleCourierRule;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierStatus;

@Service
public class DispatchService {
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final AssignmentService assignments;

    public DispatchService(OrderRepository orders, RestaurantRepository restaurants, AssignmentService assignments) {
        this.orders = orders;
        this.restaurants = restaurants;
        this.assignments = assignments;
    }

    @Transactional
    public void retryQueue() {
        var idle = new ArrayList<>(assignments.lockCouriers().stream()
                .filter(c -> c.getStatus() == CourierStatus.IDLE)
                .filter(c -> !assignments.courierHasAssignment(c.getUserId()))
                .filter(c -> orders.findFirstByCourierIdAndStatusInOrderByDateOrderedAscIdAsc(c.getUserId(),
                        List.of(OrderStatus.READY, OrderStatus.PICKED_UP)).isEmpty()).toList());
        if (idle.isEmpty()) return;
        for (Order order : orders.findByStatusAndCourierIdIsNullOrderByDateOrderedAscIdAsc(OrderStatus.READY)) {
            var courier = NearestIdleCourierRule.choose(restaurants.findById(order.getRestaurantId()).orElse(null), idle);
            if (courier.isEmpty()) return;
            assignments.offer(order, courier.get());
            idle.remove(courier.get());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryQueueAfterCommit() { retryQueue(); }
}
