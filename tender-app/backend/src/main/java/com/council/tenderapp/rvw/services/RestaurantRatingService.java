package com.council.tenderapp.rvw.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.rvw.dtos.RestaurantRatingSummary;
import com.council.tenderapp.rvw.repositories.OrderRatingRepository;

/** R-RVW-02: average restaurant stars, rounded half-up to one decimal place. */
@Service
public class RestaurantRatingService {
    private final OrderRatingRepository ratings;

    public RestaurantRatingService(OrderRatingRepository ratings) {
        this.ratings = ratings;
    }

    /** One summary per requested id; restaurants with no ratings get {@link RestaurantRatingSummary#NONE}. */
    @Transactional(readOnly = true)
    public Map<Long, RestaurantRatingSummary> summaries(Collection<Long> restaurantIds) {
        Map<Long, RestaurantRatingSummary> result = new HashMap<>();
        if (restaurantIds == null || restaurantIds.isEmpty()) {
            return result;
        }
        restaurantIds.forEach(id -> result.put(id, RestaurantRatingSummary.NONE));
        for (var totals : ratings.restaurantStarTotals(restaurantIds)) {
            result.put(totals.getRestaurantId(), summary(totals.getStarSum(), totals.getRatingCount()));
        }
        return result;
    }

    static RestaurantRatingSummary summary(long starSum, long ratingCount) {
        if (ratingCount == 0) {
            return RestaurantRatingSummary.NONE;
        }
        // Exact decimal division so e.g. 4.25 rounds to 4.3, not 4.2 as binary doubles can.
        double average = BigDecimal.valueOf(starSum)
                .divide(BigDecimal.valueOf(ratingCount), 1, RoundingMode.HALF_UP)
                .doubleValue();
        return new RestaurantRatingSummary(average, ratingCount);
    }
}
