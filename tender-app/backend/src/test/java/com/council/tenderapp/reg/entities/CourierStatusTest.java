package com.council.tenderapp.reg.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CourierStatusTest {

    @Test
    void goingOnDutyMovesOfflineToIdle() {
        assertThat(CourierStatus.OFFLINE.goOnDuty()).isEqualTo(CourierStatus.IDLE);
        assertThat(CourierStatus.IDLE.goOnDuty()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void goingOffDutyMovesIdleToOffline() {
        assertThat(CourierStatus.IDLE.goOffDuty()).isEqualTo(CourierStatus.OFFLINE);
        assertThat(CourierStatus.OFFLINE.goOffDuty()).isEqualTo(CourierStatus.OFFLINE);
    }

    @Test
    void cannotGoOffDutyMidDelivery() {
        assertThatThrownBy(CourierStatus.ASSIGNED::goOffDuty).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(CourierStatus.CARRYING::goOffDuty).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void goingOnDutyDoesNotResetAnActiveDelivery() {
        assertThatThrownBy(CourierStatus.ASSIGNED::goOnDuty).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(CourierStatus.CARRYING::goOnDuty).isInstanceOf(IllegalStateException.class);
    }
}
