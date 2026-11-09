package com.council.tenderapp.dlv.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.exceptions.InvalidCourierStateException;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@Service
public class CourierDutyService {

    private static final Logger logger = LoggerFactory.getLogger(CourierDutyService.class);

    // A courier holds an order from assignment (ready) until it is delivered
    private static final List<OrderStatus> ACTIVE_DELIVERY = List.of(OrderStatus.READY, OrderStatus.PICKED_UP);

    private final CourierProfileRepository courierProfileRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final DispatchService dispatchService;

    public CourierDutyService(CourierProfileRepository courierProfileRepository, OrderRepository orderRepository,
            RestaurantRepository restaurantRepository, DispatchService dispatchService) {
        this.courierProfileRepository = courierProfileRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.dispatchService = dispatchService;
    }

    @Transactional
    public CourierDutyResponseDto getDutyStatus(Long courierId) {
        return toResponse(findCourier(courierId));
    }

    @Transactional
    public CourierDutyResponseDto goOnDuty(Long courierId) {
        CourierProfile courier = findCourier(courierId);
        courier.setStatus(transition(() -> courier.getStatus().goOnDuty()));
        courierProfileRepository.saveAndFlush(courier);
        // A newly idle courier may be able to take an order that has been waiting (R-DLV-04)
        dispatchService.retryQueue();
        return toResponse(courier);
    }

    @Transactional
    public CourierDutyResponseDto goOffDuty(Long courierId) {
        CourierProfile courier = findCourier(courierId);
        courier.setStatus(transition(() -> courier.getStatus().goOffDuty()));
        return toResponse(courierProfileRepository.saveAndFlush(courier));
    }

    /**
     * Marks the courier who holds a picked-up order as carrying it.
     * Runs in its own transaction because order status changes are published after they commit.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startCarrying(Long orderId) {
        Long courierId = orderRepository.findById(orderId).map(Order::getCourierId).orElse(null);
        if (courierId == null) return;
        CourierProfile courier = courierProfileRepository.findWithLockByUserId(courierId).orElse(null);
        if (courier == null || courier.getStatus() != CourierStatus.ASSIGNED) return;

        courier.setStatus(courier.getStatus().pickUp());
        courierProfileRepository.saveAndFlush(courier);
        logger.info("Courier_{} picked up order #{}: new status: carrying", courierId, orderId);
    }

    /**
     * Frees the courier who held a finished order so they can take the next one.
     * Runs in its own transaction because order status changes are published after they commit.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseCourier(Long orderId) {
        Long courierId = orderRepository.findById(orderId).map(Order::getCourierId).orElse(null);
        if (courierId == null) return;
        CourierProfile courier = courierProfileRepository.findWithLockByUserId(courierId).orElse(null);
        if (courier == null || (courier.getStatus() != CourierStatus.ASSIGNED && courier.getStatus() != CourierStatus.CARRYING)) return;
        // Only free them once they hold no other active order
        if (orderRepository.findFirstByCourierIdAndStatusInOrderByDateOrderedAscIdAsc(courierId, ACTIVE_DELIVERY).isPresent()) return;

        courier.setStatus(courier.getStatus().finishDelivery());
        courierProfileRepository.saveAndFlush(courier);
        logger.info("Courier_{} finished order #{}: new status: idle", courierId, orderId);
        dispatchService.retryQueue();
    }

    private CourierDutyResponseDto toResponse(CourierProfile courier) {
        AssignedOrderDto assignedOrder = orderRepository
                .findFirstByCourierIdAndStatusInOrderByDateOrderedAscIdAsc(courier.getUserId(), ACTIVE_DELIVERY)
                .map(order -> new AssignedOrderDto(order, restaurantRepository.findById(order.getRestaurantId()).orElse(null)))
                .orElse(null);
        return new CourierDutyResponseDto(courier, assignedOrder);
    }

    // Locked so a dispatch running at the same time can't assign this courier mid-change
    private CourierProfile findCourier(Long courierId) {
        return courierProfileRepository.findWithLockByUserId(courierId)
                .orElseGet(() -> courierProfileRepository.saveAndFlush(new CourierProfile(courierId)));
    }

    private static <T> T transition(java.util.function.Supplier<T> change) {
        try {
            return change.get();
        } catch (IllegalStateException ex) {
            throw new InvalidCourierStateException(ex.getMessage());
        }
    }
}
