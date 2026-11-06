package com.council.tenderapp.dlv.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.exceptions.InvalidCourierStateException;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@Service
public class CourierDutyService {

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
