package com.council.tenderapp.rvw.services;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.services.OrderAccessPolicy;
import com.council.tenderapp.prc.exceptions.UnauthorizedOrderAccessException;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.rvw.dtos.OrderRatingResponseDto;
import com.council.tenderapp.rvw.dtos.OrderRatingViewDto;
import com.council.tenderapp.rvw.dtos.SubmitRatingRequestDto;
import com.council.tenderapp.rvw.entities.OrderRating;
import com.council.tenderapp.rvw.exceptions.InvalidRatingException;
import com.council.tenderapp.rvw.exceptions.OrderAlreadyRatedException;
import com.council.tenderapp.rvw.repositories.OrderRatingRepository;

/** R-RVW-01: a customer rates the restaurant and courier of their own delivered order, once. */
@Service
public class OrderRatingService {
    private final OrderRepository orders;
    private final OrderRatingRepository ratings;
    private final Clock clock;

    @Autowired
    public OrderRatingService(OrderRepository orders, OrderRatingRepository ratings) {
        this(orders, ratings, Clock.systemUTC());
    }

    OrderRatingService(OrderRepository orders, OrderRatingRepository ratings, Clock clock) {
        this.orders = orders;
        this.ratings = ratings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'COURIER')")
    public OrderRatingViewDto view(Long orderId, Role role, Long accountId, Long restaurantId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        boolean permitted = switch (role) {
            case CUSTOMER -> OrderAccessPolicy.isPlacedBy(order, accountId);
            case COURIER -> OrderAccessPolicy.isAssignedTo(order, accountId);
            case STAFF -> OrderAccessPolicy.isFromRestaurant(order, restaurantId);
            default -> false;
        };
        if (!permitted) {
            throw new UnauthorizedOrderAccessException("Order " + orderId + " is not available to this account.");
        }

        return ratings.findByOrderId(orderId)
                .map(rating -> role == Role.CUSTOMER
                        ? new OrderRatingViewDto(orderId, null, rating.getRestaurantStars(), rating.getCourierStars(),
                                rating.getComment(), rating.getCreatedAt())
                        : new OrderRatingViewDto(orderId,
                                role == Role.COURIER ? rating.getCourierStars() : rating.getRestaurantStars(),
                                null, null, rating.getComment(), rating.getCreatedAt()))
                .orElse(null);
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderRatingResponseDto rate(Long customerId, Long orderId, SubmitRatingRequestDto request) {
        // Locking the order serialises concurrent ratings of it; the unique order_id is the backstop.
        Order order = orders.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        if (!OrderAccessPolicy.isPlacedBy(order, customerId)) {
            throw new OrderAccessDeniedException("Order " + orderId + " does not belong to this customer");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Order " + orderId + " cannot be rated because it is "
                    + order.getStatus().name().toLowerCase(Locale.ROOT));
        }
        if (ratings.existsByOrderId(orderId)) {
            throw new OrderAlreadyRatedException(orderId);
        }
        checkCourierStars(order, request);

        OrderRating rating = new OrderRating(orderId, customerId, order.getRestaurantId(), order.getCourierId(),
                request.restaurantStars(), request.courierStars(), normalise(request.comment()), Instant.now(clock));
        try {
            return OrderRatingResponseDto.from(ratings.saveAndFlush(rating));
        } catch (DataIntegrityViolationException duplicate) {
            throw new OrderAlreadyRatedException(orderId);
        }
    }

    private static void checkCourierStars(Order order, SubmitRatingRequestDto request) {
        if (order.getCourierId() != null && request.courierStars() == null) {
            throw new InvalidRatingException("Courier rating is required for an order delivered by a courier");
        }
        if (order.getCourierId() == null && request.courierStars() != null) {
            throw new InvalidRatingException("Order " + order.getId() + " has no courier to rate");
        }
    }

    private static String normalise(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
