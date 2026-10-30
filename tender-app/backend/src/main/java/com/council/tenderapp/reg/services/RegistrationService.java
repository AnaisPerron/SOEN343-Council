package com.council.tenderapp.reg.services;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.DuplicateEmailException;
import com.council.tenderapp.reg.entities.Profile;
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

    public RegistrationService(ProfileRepository repository, PasswordEncoder passwordEncoder,
            RestaurantRepository restaurantRepository, RestaurantStaffRepository restaurantStaffRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.restaurantRepository = restaurantRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        
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
                        restaurantWithDetails(saved.getName(), request.phone(), request.address()));
                restaurantStaffRepository.saveAndFlush(new RestaurantStaff(saved.getId(), restaurant.getId()));
                return RegisterResponse.from(saved, restaurant);
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

    private static Restaurant restaurantWithDetails(String name, String phone, String address) {
        Restaurant restaurant = new Restaurant(name, "closed", null, null);
        restaurant.setPhone(phone.trim());
        restaurant.setAddress(address == null ? null : address.trim());
        return restaurant;
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        return NestedExceptionUtils.getMostSpecificCause(ex) instanceof SQLException sqlEx
                && UNIQUE_VIOLATION.equals(sqlEx.getSQLState());
    }
}
