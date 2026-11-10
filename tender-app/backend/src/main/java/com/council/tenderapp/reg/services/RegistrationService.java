package com.council.tenderapp.reg.services;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.DuplicateEmailException;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.entities.RestaurantStaff;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.repositories.RestaurantStaffRepository;
import com.council.tenderapp.reg.entities.Role;

@Service
public class RegistrationService {

    private static final String UNIQUE_VIOLATION = "23505";

    private final ProfileRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;
    private final CustomerProfileService customerProfileService;
    private final CourierProfileRepository courierProfileRepository;

    public RegistrationService(ProfileRepository repository, PasswordEncoder passwordEncoder,
            RestaurantRepository restaurantRepository, RestaurantStaffRepository restaurantStaffRepository,
            CustomerProfileService customerProfileService, CourierProfileRepository courierProfileRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.restaurantRepository = restaurantRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
        this.customerProfileService = customerProfileService;
        this.courierProfileRepository = courierProfileRepository;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.role() == Role.OPERATOR) {
            throw new AccessDeniedException("Operator accounts must be predefined");
        }
        if ((request.role() == Role.CUSTOMER || request.role() == Role.STAFF) &&
                ((request.address() != null && !request.address().isBlank() && !request.hasLocation()) ||
                 (request.hasLocation() && (request.address() == null || request.address().isBlank())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick a verified address from the suggestions");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        Profile profile = new Profile(
                request.role(),
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone().trim());

        try {
            Profile saved = repository.saveAndFlush(profile);
            if (saved.getRole() == Role.STAFF) {
                Restaurant restaurant = restaurantRepository.saveAndFlush(
                        restaurantWithDetails(saved.getName(), request));
                restaurantStaffRepository.saveAndFlush(new RestaurantStaff(saved.getId(), restaurant.getId()));
                return RegisterResponse.from(saved, restaurant);
            }
            if (saved.getRole() == Role.CUSTOMER) {
                customerProfileService.ensureExists(saved.getId());
                String address = request.address() == null || request.address().isBlank() ? null : request.address().trim();
                if (address != null || request.hasLocation()) {
                    customerProfileService.saveAddress(saved.getId(), address,
                            request.hasLocation() ? request.latitude() : null, request.hasLocation() ? request.longitude() : null);
                }
            }
            if (saved.getRole() == Role.COURIER) {
                courierProfileRepository.saveAndFlush(new CourierProfile(saved.getId()));
            }
            return RegisterResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Another request registered the same email between the check and the insert
            if (isUniqueViolation(ex)) {
                throw new DuplicateEmailException(email);
            }
            throw ex;
        }
    }

    private static Restaurant restaurantWithDetails(String name, RegisterRequest request) {
        // Without a picked address the restaurant has no location and stays off the map until one is set
        Restaurant restaurant = new Restaurant(name, "closed",
                request.hasLocation() ? request.latitude() : null, request.hasLocation() ? request.longitude() : null);
        restaurant.setPhone(request.phone().trim());
        restaurant.setAddress(request.address() == null ? null : request.address().trim());
        return restaurant;
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        return NestedExceptionUtils.getMostSpecificCause(ex) instanceof SQLException sqlEx
                && UNIQUE_VIOLATION.equals(sqlEx.getSQLState());
    }
}
