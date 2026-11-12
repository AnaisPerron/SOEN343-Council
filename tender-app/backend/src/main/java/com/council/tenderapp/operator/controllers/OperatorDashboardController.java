package com.council.tenderapp.operator.controllers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.List;
import java.util.Set;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import com.council.tenderapp.operator.dto.CourierSummary;
import com.council.tenderapp.operator.dto.CustomerSummary;
import com.council.tenderapp.operator.dto.DashboardPage;
import com.council.tenderapp.operator.dto.DashboardSummary;
import com.council.tenderapp.operator.dto.OrderSummary;
import com.council.tenderapp.operator.dto.RestaurantSummary;
import com.council.tenderapp.operator.repositories.OperatorDashboardRepository.RestaurantOption;
import com.council.tenderapp.operator.services.OperatorDashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/operator")
public class OperatorDashboardController {
    private static final Set<String> ORDER_STATUSES = Set.of("placed", "accepted", "preparing", "ready",
            "picked_up", "delivered", "cancelled", "rejected", "refunded");
    private final OperatorDashboardService service;

    public OperatorDashboardController(OperatorDashboardService service) { this.service = service; }

    @GetMapping("/summary")
    public DashboardSummary summary() { return service.summary(); }

    @GetMapping("/restaurants")
    public DashboardPage<RestaurantSummary> restaurants(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<Long> restaurantIds) {
        if (restaurantIds != null && restaurantIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid restaurant filters");
        return restaurantIds == null || restaurantIds.isEmpty() ? service.restaurants(page, size) :
                service.restaurants(page, size, restaurantIds);
    }

    @GetMapping("/restaurant-options")
    public List<RestaurantOption> restaurantOptions() { return service.restaurantOptions(); }

    @GetMapping("/orders")
    public DashboardPage<OrderSummary> orders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<Long> restaurantIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timeZone) {
        List<String> selected = statuses == null ? List.of() : statuses;
        if (selected.stream().anyMatch(status -> !ORDER_STATUSES.contains(status))
                || (restaurantIds != null && restaurantIds.stream().anyMatch(id -> id == null || id <= 0))
                || (from != null && to != null && from.isAfter(to))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order filters");
        }
        if (selected.isEmpty() && (restaurantIds == null || restaurantIds.isEmpty()) && from == null && to == null)
            return service.orders(page, size);
        ZoneId zone;
        try { zone = ZoneId.of(timeZone); }
        catch (DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time zone");
        }
        return service.orders(page, size, selected, restaurantIds == null ? List.of() : restaurantIds, from, to, zone);
    }

    @GetMapping("/customers")
    public DashboardPage<CustomerSummary> customers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.customers(page, size);
    }

    @GetMapping("/couriers")
    public DashboardPage<CourierSummary> couriers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.couriers(page, size);
    }
}
