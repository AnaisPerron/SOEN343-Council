package com.council.tenderapp.reg.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.dtos.UpdateProfileRequest;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.entities.RestaurantStaff;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.repositories.RestaurantStaffRepository;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {
    @Mock
    private ProfileRepository profiles;

    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantStaffRepository restaurantStaff;

    private ProfileController controller;
    private Profile profile;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(profiles, restaurants, restaurantStaff);
        profile = new Profile(Role.CUSTOMER, "Jane", "jane@example.com", "saved-hash", "514-555-0101");
        ReflectionTestUtils.setField(profile, "id", 7L);
        authentication = new UsernamePasswordAuthenticationToken(7L, null);
    }

    @Test
    void readsTheAuthenticatedUsersProfile() {
        when(profiles.findById(7L)).thenReturn(Optional.of(profile));

        RegisterResponse response = controller.get(authentication);

        assertThat(response.name()).isEqualTo("Jane");
        assertThat(response.email()).isEqualTo("jane@example.com");
        verify(profiles).findById(7L);
    }

    @Test
    void savesEditableFieldsWithoutChangingEmailOrPassword() {
        when(profiles.findById(7L)).thenReturn(Optional.of(profile));
        when(profiles.save(profile)).thenReturn(profile);

        RegisterResponse response = controller.update(authentication,
                new UpdateProfileRequest(" Jane Lee ", " 514-555-0199 "));

        verify(profiles).save(profile);
        assertThat(response.name()).isEqualTo("Jane Lee");
        assertThat(response.phone()).isEqualTo("514-555-0199");
        assertThat(profile.getEmail()).isEqualTo("jane@example.com");
        assertThat(profile.getPasswordHash()).isEqualTo("saved-hash");
    }

    @Test
    void rejectsRequestsWithoutAnAuthenticatedAccountId() {
        assertThatThrownBy(() -> controller.get(new UsernamePasswordAuthenticationToken("other", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void restaurantProfileReadsAndUpdatesRestaurantDetails() {
        Profile account = new Profile(Role.STAFF, "Sam Staff", "sam@example.com", "saved-hash", "514-555-0101");
        ReflectionTestUtils.setField(account, "id", 8L);
        Restaurant restaurant = new Restaurant("Maple & Main", "open", null, null);
        restaurant.setId(42L);
        restaurant.setAddress("123 Main St, Montreal, Canada");
        when(profiles.findById(8L)).thenReturn(Optional.of(account));
        when(restaurantStaff.findById(8L)).thenReturn(Optional.of(new RestaurantStaff(8L, 42L)));
        when(restaurants.findById(42L)).thenReturn(Optional.of(restaurant));
        when(profiles.save(account)).thenReturn(account);
        UsernamePasswordAuthenticationToken staffAuth = new UsernamePasswordAuthenticationToken(8L, null);

        RegisterResponse before = controller.get(staffAuth);
        RegisterResponse after = controller.update(staffAuth,
                new UpdateProfileRequest("Maple Bistro", "514-555-0199", "99 Rue St, Montreal"));

        assertThat(before.name()).isEqualTo("Maple & Main");
        assertThat(before.restaurantId()).isEqualTo(42L);
        assertThat(after.name()).isEqualTo("Maple Bistro");
        assertThat(after.address()).isEqualTo("99 Rue St, Montreal");
        assertThat(restaurant.getPhone()).isEqualTo("514-555-0199");
        verify(restaurants).save(restaurant);
    }
}
