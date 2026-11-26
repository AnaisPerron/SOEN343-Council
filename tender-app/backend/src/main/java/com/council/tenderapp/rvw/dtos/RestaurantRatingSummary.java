package com.council.tenderapp.rvw.dtos;

/**
 * Average restaurant stars over a restaurant's ratings (R-RVW-02).
 * averageRating is null when the restaurant has no ratings yet.
 */
public record RestaurantRatingSummary(Double averageRating, long ratingCount) {
    public static final RestaurantRatingSummary NONE = new RestaurantRatingSummary(null, 0);
}
