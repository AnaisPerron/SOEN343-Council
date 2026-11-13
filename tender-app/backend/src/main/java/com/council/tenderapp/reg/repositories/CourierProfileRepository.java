package com.council.tenderapp.reg.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;

import jakarta.persistence.LockModeType;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CourierProfile> findAllByOrderByUserIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CourierProfile> findWithLockByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CourierProfile> findByStatusOrderByUserIdAsc(CourierStatus status);
}
