package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import com.council.tenderapp.dlv.domain.NearestIdleCourierRule;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;

class NearestIdleCourierRuleTest {
    private final Restaurant restaurant = new Restaurant("Cafe", "open", 45.5, -73.5);

    private CourierProfile courier(long id, CourierStatus status, Double lat, Double lon) {
        var courier = new CourierProfile(id);
        courier.setStatus(status);
        courier.setLatitude(lat);
        courier.setLongitude(lon);
        return courier;
    }

    @Test
    void choosesTheNearestIdleCourierRatherThanTheLowestId() {
        var far = courier(1, CourierStatus.IDLE, 45.8, -73.9);
        var near = courier(9, CourierStatus.IDLE, 45.501, -73.501);
        assertThat(NearestIdleCourierRule.choose(restaurant, List.of(far, near))).contains(near);
        assertThat(near.getStatus()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void tiesAlwaysUseLowestIdRegardlessOfInputOrder() {
        var candidates = new ArrayList<>(List.of(courier(9, CourierStatus.IDLE, 45.5, -73.5),
                courier(2, CourierStatus.IDLE, 45.5, -73.5), courier(5, CourierStatus.IDLE, 45.5, -73.5)));
        for (int seed = 0; seed < 20; seed++) {
            Collections.shuffle(candidates, new Random(seed));
            assertThat(NearestIdleCourierRule.choose(restaurant, candidates).orElseThrow().getUserId()).isEqualTo(2L);
        }
    }

    @Test
    void ignoresOfflineAssignedAndCarryingCouriersEvenWhenCloser() {
        var available = courier(9, CourierStatus.IDLE, 45.8, -73.8);
        var candidates = List.of(courier(1, CourierStatus.OFFLINE, 45.5, -73.5),
                courier(2, CourierStatus.ASSIGNED, 45.5, -73.5), courier(3, CourierStatus.CARRYING, 45.5, -73.5), available);
        assertThat(NearestIdleCourierRule.choose(restaurant, candidates)).contains(available);
        assertThat(NearestIdleCourierRule.choose(restaurant, candidates.subList(0, 3))).isEmpty();
        assertThat(NearestIdleCourierRule.choose(restaurant, List.of())).isEmpty();
    }

    @Test
    void knownLocationsTakePriorityAndUnknownLocationsFallBackDeterministically() {
        var unknown = courier(1, CourierStatus.IDLE, null, null);
        var invalid = courier(2, CourierStatus.IDLE, Double.NaN, -73.5);
        var known = courier(9, CourierStatus.IDLE, 46.0, -73.5);
        assertThat(NearestIdleCourierRule.choose(restaurant, List.of(unknown, invalid, known))).contains(known);
        assertThat(NearestIdleCourierRule.choose(restaurant, List.of(invalid, unknown))).contains(unknown);
        assertThat(NearestIdleCourierRule.choose(null, List.of(known, unknown))).contains(unknown);
    }
}
