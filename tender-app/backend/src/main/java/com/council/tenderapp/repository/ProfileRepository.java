package com.council.tenderapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
