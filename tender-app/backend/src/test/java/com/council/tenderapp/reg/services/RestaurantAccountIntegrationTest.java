package com.council.tenderapp.reg.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.repositories.RestaurantStaffRepository;
import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.LoginResponse;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.entities.Role;

@SpringBootTest
@Transactional
class RestaurantAccountIntegrationTest {
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private SessionService sessionService;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private RestaurantStaffRepository restaurantStaffRepository;

    @Test
    void newlyRegisteredRestaurantCanLogInWithItsOwnRestaurant() {
        RegisterResponse registered = registrationService.register(new RegisterRequest(
                "Bistro", "bistro-integration@example.com", "secret123",
                "514-555-0199", Role.STAFF, "123 Main St, Montreal, Canada", 45.5, -73.6));

        LoginResponse login = loginService.login(new LoginRequest(
                "bistro-integration@example.com", "secret123"));

        Long restaurantId = sessionService.find(login.token()).orElseThrow().restaurantId();
        assertThat(restaurantId).isEqualTo(
                restaurantStaffRepository.findById(registered.id()).orElseThrow().getRestaurantId());
        assertThat(restaurantRepository.findById(restaurantId).orElseThrow().getName()).isEqualTo("Bistro");
        assertThat(restaurantRepository.findById(restaurantId).orElseThrow().getPhone()).isEqualTo("514-555-0199");
        assertThat(login.name()).isEqualTo("Bistro");
        assertThat(login.address()).isEqualTo("123 Main St, Montreal, Canada");
    }
}
