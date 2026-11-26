package com.council.tenderapp.ord.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.ord.dtos.OrderLineResponse;
import com.council.tenderapp.ord.dtos.OrderResponse;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.repositories.ProfileRepository;

/**
 * Builds the restaurant and courier order views: customer contact, courier name and order lines.
 * Extracted from {@link OrderService} so that the service keeps only the order lifecycle.
 */
class OrderResponseAssembler {
    private final ProfileRepository profileRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final OrderEventRepository orderEventRepository;
    private final AssignmentRepository assignmentHistory;

    OrderResponseAssembler(ProfileRepository profileRepository, CourierProfileRepository courierProfileRepository,
            OrderEventRepository orderEventRepository, AssignmentRepository assignmentHistory) {
        this.profileRepository = profileRepository;
        this.courierProfileRepository = courierProfileRepository;
        this.orderEventRepository = orderEventRepository;
        this.assignmentHistory = assignmentHistory;
    }

    OrderResponse toResponse(Order order) {
        Profile customer = order.getCustomerId() == null
            ? null
            : profileRepository.findById(order.getCustomerId()).orElse(null);
        Profile courier = order.getCourierId() == null
            ? null
            : profileRepository.findById(order.getCourierId()).orElse(null);
        CourierProfile courierProfile = order.getCourierId() == null
            ? null
            : courierProfileRepository.findById(order.getCourierId()).orElse(null);
        boolean acceptedEvent = order.getId() != null
            && orderEventRepository.existsByOrderIdAndEventType(order.getId(), "courier_accepted");
        Assignment latest = order.getId() == null || order.getCourierId() == null ? null : assignmentHistory
                .findFirstByOrderIdAndCourierIdOrderByAssignedAtDescIdDesc(order.getId(), order.getCourierId()).orElse(null);
        return toResponse(order, customer, courierName(order, courier, courierProfile, acceptedEvent, latest));
    }

    // Load customer and courier profiles in batches so the restaurant history stays efficient.
    List<OrderResponse> toResponses(List<Order> orders) {
        List<Long> customerIds = orders.stream()
            .map(Order::getCustomerId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<Long> courierIds = orders.stream()
            .map(Order::getCourierId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<Long> profileIds = java.util.stream.Stream.concat(customerIds.stream(), courierIds.stream())
            .distinct()
            .toList();
        Map<Long, Profile> profiles = profileIds.isEmpty()
            ? Map.of()
            : profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        Map<Long, CourierProfile> courierProfiles = courierIds.isEmpty()
            ? Map.of()
            : courierProfileRepository.findAllById(courierIds).stream()
                .collect(Collectors.toMap(CourierProfile::getUserId, Function.identity()));
        Set<Long> acceptedOrderIds = orders.isEmpty()
            ? Set.of()
            : orderEventRepository.findByOrderIdInAndEventType(
                orders.stream().map(Order::getId).toList(), "courier_accepted"
            ).stream().map(event -> event.getOrderId())
                .collect(Collectors.toSet());
        Map<AssignmentKey, Assignment> latestAssignments = new java.util.HashMap<>();
        if (!orders.isEmpty()) {
            assignmentHistory.findByOrderIdInOrderByAssignedAtDescIdDesc(orders.stream().map(Order::getId).toList())
                    .forEach(a -> latestAssignments.putIfAbsent(new AssignmentKey(a.getOrderId(), a.getCourierId()), a));
        }
        return orders.stream()
            .map(order -> toResponse(
                order,
                order.getCustomerId() == null ? null : profiles.get(order.getCustomerId()),
                courierName(
                    order,
                    order.getCourierId() == null ? null : profiles.get(order.getCourierId()),
                    order.getCourierId() == null ? null : courierProfiles.get(order.getCourierId()),
                    acceptedOrderIds.contains(order.getId()),
                    latestAssignments.get(new AssignmentKey(order.getId(), order.getCourierId()))
                )
            ))
            .toList();
    }

    private record AssignmentKey(Long orderId, Long courierId) {}

    private static String courierName(Order order, Profile courier, CourierProfile courierProfile, boolean acceptedEvent, Assignment latest) {
        if (courier == null || courierProfile == null) return null;
        if (latest != null) return latest.getAcceptedAt() == null ? null : courier.getName();
        boolean accepted = acceptedEvent
            || courierProfile.getStatus() == CourierStatus.CARRYING
            || order.getStatus() == OrderStatus.PICKED_UP
            || order.getStatus() == OrderStatus.DELIVERED
            || order.getStatus() == OrderStatus.REFUNDED;
        return accepted ? courier.getName() : null;
    }

    private OrderResponse toResponse(Order order, Profile customer, String courierName) {
        List<OrderLineResponse> orderLines = order.getOrderLines().stream()
            .map(line ->
                new OrderLineResponse(line.getMenuItem().getName(), line.getQuantity(), line.getUnitPrice())
            )
            .toList();
        return new OrderResponse(
            order.getId(),
            order.getRestaurantId(),
            order.getCourierId(),
            courierName,
            order.getStatus(),
            order.getDateOrdered(),
            order.getAcceptedAt(),
            order.getRejectionReason(),
            order.getTotalPrice(),
            customer == null ? null : customer.getName(),
            customer == null ? null : customer.getPhone(),
            order.getDeliveryAddress(),
            orderLines
        );
    }
}
