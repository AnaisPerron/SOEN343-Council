package com.council.tenderapp.dlv.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.technicalservices.logging.AppLogService;

/**
 * The dispatch queue is every ready order that has no courier yet, oldest first.
 * Retrying it hands each waiting order to the nearest idle courier until couriers run out (R-DLV-04).
 */
@Service
public class DispatchService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final AppLogService logService;

    public DispatchService(OrderRepository orderRepository, RestaurantRepository restaurantRepository,
            CourierProfileRepository courierProfileRepository, AppLogService logService) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.courierProfileRepository = courierProfileRepository;
        this.logService = logService;
    }

    @Transactional
    public void retryQueue() {
        List<CourierProfile> idle = new ArrayList<>(courierProfileRepository.findByStatusOrderByUserIdAsc(CourierStatus.IDLE));
        for (Order order : orderRepository.findWaitingReadyOrdersForDispatch()) {
            if (idle.isEmpty()) {
                logService.info("Dispatch queue: order #" + order.getId() + " is waiting for a courier");
                continue;
            }
            Restaurant restaurant = restaurantRepository.findById(order.getRestaurantId()).orElse(null);
            CourierProfile courier = idle.stream().min(byDistanceTo(restaurant)).orElseThrow();
            idle.remove(courier);
            order.setCourierId(courier.getUserId());
            courier.setStatus(CourierStatus.ASSIGNED);
            logService.info("Dispatch queue: order #" + order.getId() + " assigned to courier #" + courier.getUserId()
                    + " and removed from the queue");
        }
    }

    // Couriers without a known location (or a restaurant without one) fall back to lowest id
    private static Comparator<CourierProfile> byDistanceTo(Restaurant restaurant) {
        return Comparator.comparingDouble((CourierProfile courier) -> distanceKm(restaurant, courier))
                .thenComparing(CourierProfile::getUserId);
    }

    private static double distanceKm(Restaurant restaurant, CourierProfile courier) {
        if (restaurant == null || restaurant.getLatitude() == null || restaurant.getLongitude() == null
                || courier.getLatitude() == null || courier.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        double lat1 = Math.toRadians(restaurant.getLatitude());
        double lat2 = Math.toRadians(courier.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(courier.getLongitude() - restaurant.getLongitude());
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371 * Math.asin(Math.sqrt(a));
    }
}
