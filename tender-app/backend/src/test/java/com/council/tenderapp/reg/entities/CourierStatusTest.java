package com.council.tenderapp.reg.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CourierStatusTest {

    @Test
    void offlineCourierCanGoOnDuty() {
        assertThat(CourierStatus.OFFLINE.goOnDuty()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void idleCourierCanGoOffDuty() {
        assertThat(CourierStatus.IDLE.goOffDuty()).isEqualTo(CourierStatus.OFFLINE);
    }

    @Test
    void carryingCourierCannotGoOffDuty() {
        assertThatThrownBy(CourierStatus.CARRYING::goOffDuty).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assignedCourierCannotGoOffDuty() {
        assertThatThrownBy(CourierStatus.ASSIGNED::goOffDuty).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void repeatingTheCurrentDutyStatusChangesNothing() {
        assertThat(CourierStatus.IDLE.goOnDuty()).isEqualTo(CourierStatus.IDLE);
        assertThat(CourierStatus.OFFLINE.goOffDuty()).isEqualTo(CourierStatus.OFFLINE);
    }

    @Test
    void pickingUpTheOrderMakesTheCourierCarryIt() {
        assertThat(CourierStatus.ASSIGNED.pickUp()).isEqualTo(CourierStatus.CARRYING);
        assertThat(CourierStatus.CARRYING.pickUp()).isEqualTo(CourierStatus.CARRYING);
    }

    @Test
    void cannotPickUpWithoutAnAssignedOrder() {
        assertThatThrownBy(CourierStatus.IDLE::pickUp).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(CourierStatus.OFFLINE::pickUp).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void finishingADeliveryReturnsTheCourierToIdle() {
        assertThat(CourierStatus.ASSIGNED.finishDelivery()).isEqualTo(CourierStatus.IDLE);
        assertThat(CourierStatus.CARRYING.finishDelivery()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void cannotFinishADeliveryWithoutOne() {
        assertThatThrownBy(CourierStatus.IDLE::finishDelivery).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(CourierStatus.OFFLINE::finishDelivery).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void goingOnDutyDoesNotResetAnActiveDelivery() {
        assertThatThrownBy(CourierStatus.ASSIGNED::goOnDuty).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(CourierStatus.CARRYING::goOnDuty).isInstanceOf(IllegalStateException.class);
    }
}
