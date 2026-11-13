package com.council.tenderapp.prc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.prc.entities.Bill;
import com.council.tenderapp.prc.repositories.BillRepository;

@Service
public class PricingService {
    private final RestaurantRepository restaurants;
    private final BillRepository bills;
    private final BigDecimal deliveryRate;
    private final BigDecimal serviceFee;
    private final BigDecimal smallOrderThreshold;
    private final BigDecimal smallOrderSurcharge;

    public PricingService(RestaurantRepository restaurants, BillRepository bills,
            @Value("${app.pricing.delivery-fee-per-km}") BigDecimal deliveryRate,
            @Value("${app.pricing.service-fee}") BigDecimal serviceFee,
            @Value("${app.pricing.small-order-threshold}") BigDecimal smallOrderThreshold,
            @Value("${app.pricing.small-order-surcharge}") BigDecimal smallOrderSurcharge) {
        this.restaurants = restaurants;
        this.bills = bills;
        this.deliveryRate = deliveryRate;
        this.serviceFee = serviceFee;
        this.smallOrderThreshold = smallOrderThreshold;
        this.smallOrderSurcharge = smallOrderSurcharge;
    }

    public record Quote(BigDecimal subtotal, BigDecimal distanceKm, BigDecimal serviceFee,
            BigDecimal deliveryFee, BigDecimal smallOrderFee, BigDecimal total) {}

    public record Rules(BigDecimal deliveryFeePerKm, BigDecimal serviceFee,
            BigDecimal smallOrderThreshold, BigDecimal smallOrderSurcharge) {}

    public Rules rules() {
        return new Rules(deliveryRate, serviceFee, smallOrderThreshold, smallOrderSurcharge);
    }

    public Quote quote(BigDecimal subtotal, Long restaurantId, Double latitude, Double longitude) {
        Restaurant restaurant = restaurants.findById(restaurantId)
                .orElseThrow(() -> new InvalidOrderException("Restaurant is unavailable for pricing"));
        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null || latitude == null || longitude == null) {
            throw new InvalidOrderException("Restaurant and delivery address must have verified map locations");
        }
        double distance = haversineKm(restaurant.getLatitude(), restaurant.getLongitude(), latitude, longitude);
        BigDecimal km = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal delivery = km.multiply(deliveryRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal service = serviceFee.setScale(2, RoundingMode.HALF_UP);
        BigDecimal small = subtotal.compareTo(smallOrderThreshold) < 0
                ? smallOrderSurcharge.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        BigDecimal items = subtotal.setScale(2, RoundingMode.HALF_UP);
        return new Quote(items, km, service, delivery, small, items.add(service).add(delivery).add(small));
    }

    public void saveBill(Order order, Quote quote) {
        Bill bill = new Bill();
        bill.setOrderId(order.getId());
        bill.setCustomerId(order.getCustomerId());
        bill.setDate(OffsetDateTime.now());
        bill.setAmount(quote.subtotal().doubleValue());
        bill.setServiceFee(quote.serviceFee().doubleValue());
        bill.setDeliveryFee(quote.deliveryFee().doubleValue());
        bill.setSmallOrderFee(quote.smallOrderFee().doubleValue());
        bills.save(bill);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double latitudeDelta = Math.toRadians(lat2 - lat1);
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0, 1 - a)));
    }
}
