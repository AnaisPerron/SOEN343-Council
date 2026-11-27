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
import com.council.tenderapp.rvw.dtos.OrderRatingResponseDto;
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

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderRatingResponseDto rate(Long customerId, Long orderId, SubmitRatingRequestDto request) {
        // Locking the order serialises concurrent ratings of it; the unique order_id is the backstop.
        Order order = orders.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        if (customerId == null || !customerId.equals(order.getCustomerId())) {
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
