package com.council.tenderapp.dlv.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;

public final class NearestIdleCourierRule {
    private NearestIdleCourierRule() {}

    public static Optional<CourierProfile> choose(Restaurant restaurant, Collection<CourierProfile> couriers) {
        return couriers.stream().filter(c -> c.getStatus() == CourierStatus.IDLE)
                .min(Comparator.comparingDouble((CourierProfile c) -> distanceKm(restaurant, c))
                        .thenComparing(CourierProfile::getUserId));
    }

    private static double distanceKm(Restaurant restaurant, CourierProfile courier) {
        if (restaurant == null || !valid(restaurant.getLatitude(), restaurant.getLongitude())
                || !valid(courier.getLatitude(), courier.getLongitude())) return Double.MAX_VALUE;
        double lat1 = Math.toRadians(restaurant.getLatitude());
        double lat2 = Math.toRadians(courier.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(courier.getLongitude() - restaurant.getLongitude());
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
    }

    private static boolean valid(Double latitude, Double longitude) {
        return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
    }
}
