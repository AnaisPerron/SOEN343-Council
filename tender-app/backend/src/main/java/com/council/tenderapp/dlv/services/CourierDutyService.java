package com.council.tenderapp.dlv.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.exceptions.InvalidCourierStateException;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@Service
public class CourierDutyService {

    private final CourierProfileRepository courierProfileRepository;
    private final DispatchService dispatchService;

    public CourierDutyService(CourierProfileRepository courierProfileRepository, DispatchService dispatchService) {
        this.courierProfileRepository = courierProfileRepository;
        this.dispatchService = dispatchService;
    }

    @Transactional
    public CourierDutyResponseDto goOnDuty(Long courierId) {
        CourierProfile courier = findCourier(courierId);
        courier.setStatus(transition(() -> courier.getStatus().goOnDuty()));
        courierProfileRepository.saveAndFlush(courier);
        // A newly idle courier may be able to take an order that has been waiting (R-DLV-04)
        dispatchService.retryQueue();
        return new CourierDutyResponseDto(courier);
    }

    @Transactional
    public CourierDutyResponseDto goOffDuty(Long courierId) {
        CourierProfile courier = findCourier(courierId);
        courier.setStatus(transition(() -> courier.getStatus().goOffDuty()));
        return new CourierDutyResponseDto(courierProfileRepository.saveAndFlush(courier));
    }

    // Locked so a dispatch running at the same time can't assign this courier mid-change
    private CourierProfile findCourier(Long courierId) {
        return courierProfileRepository.findWithLockByUserId(courierId)
                .orElseGet(() -> courierProfileRepository.saveAndFlush(new CourierProfile(courierId)));
    }

    private static <T> T transition(java.util.function.Supplier<T> change) {
        try {
            return change.get();
        } catch (IllegalStateException ex) {
            throw new InvalidCourierStateException(ex.getMessage());
        }
    }
}
