package com.council.tenderapp.operator.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.operator.dto.CourierSummary;
import com.council.tenderapp.operator.dto.CustomerSummary;
import com.council.tenderapp.operator.dto.DashboardPage;
import com.council.tenderapp.operator.dto.DashboardSummary;
import com.council.tenderapp.operator.dto.OrderSummary;
import com.council.tenderapp.operator.dto.RestaurantSummary;
import com.council.tenderapp.operator.repositories.OperatorDashboardRepository;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('OPERATOR')")
public class OperatorDashboardService {
    private final OperatorDashboardRepository repository;

    public OperatorDashboardService(OperatorDashboardRepository repository) {
        this.repository = repository;
    }

    public DashboardSummary summary() { return repository.summary(); }
    public DashboardPage<RestaurantSummary> restaurants(int page, int size) { return repository.restaurants(page, size); }
    public DashboardPage<RestaurantSummary> restaurants(int page, int size, List<Long> ids) { return repository.restaurants(page, size, ids); }
    public List<OperatorDashboardRepository.RestaurantOption> restaurantOptions() { return repository.restaurantOptions(); }
    public DashboardPage<OrderSummary> orders(int page, int size) { return repository.orders(page, size); }
    public DashboardPage<OrderSummary> orders(int page, int size, List<String> statuses,
            List<Long> restaurantIds, LocalDate from, LocalDate to, ZoneId timeZone) {
        return repository.orders(page, size, statuses, restaurantIds, from, to, timeZone);
    }
    public DashboardPage<CustomerSummary> customers(int page, int size) { return repository.customers(page, size); }
    public DashboardPage<CourierSummary> couriers(int page, int size) { return repository.couriers(page, size); }
}
