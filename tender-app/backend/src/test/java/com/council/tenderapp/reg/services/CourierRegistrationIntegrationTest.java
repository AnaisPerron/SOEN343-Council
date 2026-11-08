package com.council.tenderapp.reg.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@SpringBootTest
@Transactional
class CourierRegistrationIntegrationTest {
    @Autowired private RegistrationService registrationService;
    @Autowired private CourierProfileRepository courierProfileRepository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void newCourierIsStoredOfflineWithoutLocation() {
        RegisterResponse registered = registrationService.register(new RegisterRequest(
                "Nora", "nora-integration@example.com", "secret123", "514-555-0107", Role.COURIER));

        CourierProfile courier = courierProfileRepository.findById(registered.id()).orElseThrow();
        assertThat(courier.getStatus()).isEqualTo(CourierStatus.OFFLINE);
        assertThat(courier.getLatitude()).isNull();
        assertThat(courier.getLongitude()).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM courier_profiles WHERE user_id = ?", String.class, registered.id()))
                .isEqualTo("offline");
    }
}
