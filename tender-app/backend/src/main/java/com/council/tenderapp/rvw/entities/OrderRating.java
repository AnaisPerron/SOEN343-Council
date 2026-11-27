package com.council.tenderapp.rvw.entities;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One customer rating of a delivered order (R-RVW-01). The unique order_id makes
 * "once per order" hold in the database even if two requests race.
 * Restaurant and courier ids are copied from the order so averages (R-RVW-02)
 * do not depend on the order row keeping its courier.
 */
@Entity
@Table(name = "order_ratings", uniqueConstraints = @UniqueConstraint(name = "uq_order_ratings_order", columnNames = "order_id"))
public class OrderRating {
    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 5;
    public static final int MAX_COMMENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(name = "restaurant_stars", nullable = false)
    private Integer restaurantStars;

    @Column(name = "courier_stars")
    private Integer courierStars;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderRating() {
    }

    public OrderRating(Long orderId, Long customerId, Long restaurantId, Long courierId,
            int restaurantStars, Integer courierStars, String comment, Instant createdAt) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customerId = Objects.requireNonNull(customerId);
        this.restaurantId = Objects.requireNonNull(restaurantId);
        this.courierId = courierId;
        this.restaurantStars = requireStars(restaurantStars);
        this.courierStars = courierStars == null ? null : requireStars(courierStars);
        if ((courierId == null) != (courierStars == null)) {
            throw new IllegalArgumentException("Courier stars must be given exactly when the order had a courier");
        }
        this.comment = comment;
        this.createdAt = Objects.requireNonNull(createdAt).truncatedTo(ChronoUnit.MICROS);
    }

    private static int requireStars(int stars) {
        if (stars < MIN_STARS || stars > MAX_STARS) {
            throw new IllegalArgumentException("Stars must be between " + MIN_STARS + " and " + MAX_STARS);
        }
        return stars;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public Long getRestaurantId() { return restaurantId; }
    public Long getCourierId() { return courierId; }
    public Integer getRestaurantStars() { return restaurantStars; }
    public Integer getCourierStars() { return courierStars; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
