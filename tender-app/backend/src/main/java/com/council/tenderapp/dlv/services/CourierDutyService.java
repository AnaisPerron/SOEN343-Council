package com.council.tenderapp.dlv.services;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.exceptions.InvalidCourierStateException;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@Service
public class CourierDutyService {

    private static final Logger logger = LoggerFactory.getLogger(CourierDutyService.class);

    @PersistenceContext
    private EntityManager entityManager;

    // An offer is READY and an accepted or carried delivery is PICKED_UP.
    private static final List<OrderStatus> ACTIVE_DELIVERY = List.of(OrderStatus.READY, OrderStatus.PICKED_UP);

    private final CourierProfileRepository courierProfileRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderEventRepository orderEventRepository;
    private final DispatchService dispatchService;

    public CourierDutyService(CourierProfileRepository courierProfileRepository, OrderRepository orderRepository,
            RestaurantRepository restaurantRepository, OrderEventRepository orderEventRepository, DispatchService dispatchService) {
        this.courierProfileRepository = courierProfileRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderEventRepository = orderEventRepository;
        this.dispatchService = dispatchService;
    }

    @Transactional
    public CourierDutyResponseDto getDutyStatus(Long courierId) {
        CourierProfile courier = findCourier(courierId);
        entityManager.refresh(courier);
        return toResponse(courier);
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
        boolean withdrewOffer = courier.getStatus() == CourierStatus.IDLE && withdrawOffer(courierId, null);
        courier.setStatus(transition(() -> courier.getStatus().goOffDuty()));
        courierProfileRepository.saveAndFlush(courier);
        if (withdrewOffer) dispatchService.retryQueue();
        return toResponse(courier);
    }

    @Transactional
    public CourierDutyResponseDto acceptOffer(Long courierId, Long orderId) {
        CourierProfile courier = findCourier(courierId);
        if (courier.getStatus() != CourierStatus.IDLE || !isOffer(courierId, orderId)) {
            throw new InvalidCourierStateException("Order is not an available offer for this courier");
        }
        courier.setStatus(CourierStatus.ASSIGNED);
        courierProfileRepository.saveAndFlush(courier);
        orderEventRepository.save(new OrderEvent(orderId, "ready", "ready", "courier_accepted", Instant.now()));
        logger.info("Courier_{} accepted order #{}: new status: assigned", courierId, orderId);
        return toResponse(courier);
    }

    @Transactional
    public CourierDutyResponseDto declineOffer(Long courierId, Long orderId) {
        CourierProfile courier = findCourier(courierId);
        if (courier.getStatus() != CourierStatus.IDLE || !withdrawOffer(courierId, orderId)) {
            throw new InvalidCourierStateException("Order is not an available offer for this courier");
        }
        courier.setStatus(transition(() -> courier.getStatus().goOffDuty()));
        courierProfileRepository.saveAndFlush(courier);
        logger.info("Courier_{} declined order #{} and went off duty", courierId, orderId);
        dispatchService.retryQueue();
        return toResponse(courier);
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
        Order order = orderRepository.findById(orderId).orElse(null);
        Long courierId = order == null ? null : order.getCourierId();
        if (courierId == null) return;
        CourierProfile courier = courierProfileRepository.findWithLockByUserId(courierId).orElse(null);
        if (courier == null) return;
        if (courier.getStatus() == CourierStatus.IDLE && order.getStatus() != OrderStatus.READY
                && order.getStatus() != OrderStatus.PICKED_UP) {
            order.setCourierId(null);
            orderRepository.saveAndFlush(order);
            logger.info("Courier_{} offer for order #{} was withdrawn", courierId, orderId);
            dispatchService.retryQueue();
            return;
        }
        if (courier.getStatus() != CourierStatus.ASSIGNED && courier.getStatus() != CourierStatus.CARRYING) return;
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

    private boolean isOffer(Long courierId, Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .filter(order -> order.getStatus() == OrderStatus.READY && courierId.equals(order.getCourierId()))
                .isPresent();
    }

    private boolean withdrawOffer(Long courierId, Long requestedOrderId) {
        Order offer = orderRepository.findFirstByCourierIdAndStatusInOrderByDateOrderedAscIdAsc(
                courierId, List.of(OrderStatus.READY)).orElse(null);
        if (offer == null || (requestedOrderId != null && !requestedOrderId.equals(offer.getId()))) return false;
        Order lockedOffer = orderRepository.findByIdForUpdate(offer.getId()).orElse(null);
        if (lockedOffer == null || lockedOffer.getStatus() != OrderStatus.READY
                || !courierId.equals(lockedOffer.getCourierId())) return false;
        lockedOffer.setCourierId(null);
        orderRepository.saveAndFlush(lockedOffer);
        return true;
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
