package com.council.tenderapp.reg.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.reg.entities.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Profile> findByEmailIgnoreCase(String email);
}
