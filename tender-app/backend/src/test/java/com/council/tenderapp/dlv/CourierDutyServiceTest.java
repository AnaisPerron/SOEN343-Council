package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.exceptions.InvalidCourierStateException;
import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@ExtendWith(MockitoExtension.class)
class CourierDutyServiceTest {
    private static final long COURIER_ID = 6L;

    @Mock private CourierProfileRepository couriers;
    @Mock private OrderEventRepository orderEvents;
    @Mock private OrderRepository orders;
    @Mock private RestaurantRepository restaurants;
    @Mock private DispatchService dispatchService;

    private CourierDutyService service;

    @BeforeEach
    void setUp() {
        service = new CourierDutyService(couriers, orders, restaurants, orderEvents, dispatchService);
    }

    private CourierProfile courierIn(CourierStatus status) {
        CourierProfile courier = new CourierProfile(COURIER_ID);
        courier.setStatus(status);
        when(couriers.findWithLockByUserId(COURIER_ID)).thenReturn(Optional.of(courier));
        return courier;
    }

    private void savesReturnTheCourier() {
        when(couriers.saveAndFlush(any(CourierProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void offlineCourierCanGoOnDuty() {
        CourierProfile courier = courierIn(CourierStatus.OFFLINE);
        savesReturnTheCourier();

        CourierDutyResponseDto response = service.goOnDuty(COURIER_ID);

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.IDLE);
        assertThat(response.status()).isEqualTo("idle");
        verify(couriers).saveAndFlush(courier);
        verify(dispatchService).retryQueue();
    }

    @Test
    void idleCourierCanGoOffDuty() {
        CourierProfile courier = courierIn(CourierStatus.IDLE);
        savesReturnTheCourier();

        CourierDutyResponseDto response = service.goOffDuty(COURIER_ID);

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.OFFLINE);
        assertThat(response.status()).isEqualTo("offline");
        verify(couriers).saveAndFlush(courier);
    }

    @Test
    void carryingCourierCannotGoOffDuty() {
        CourierProfile courier = courierIn(CourierStatus.CARRYING);

        assertThatThrownBy(() -> service.goOffDuty(COURIER_ID))
                .isInstanceOf(InvalidCourierStateException.class)
                .hasMessageContaining("carrying");

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.CARRYING);
        verify(couriers, never()).saveAndFlush(any());
    }

    @Test
    void assignedCourierCannotGoOffDuty() {
        CourierProfile courier = courierIn(CourierStatus.ASSIGNED);

        assertThatThrownBy(() -> service.goOffDuty(COURIER_ID))
                .isInstanceOf(InvalidCourierStateException.class)
                .hasMessageContaining("assigned");

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.ASSIGNED);
        verify(couriers, never()).saveAndFlush(any());
    }
}
