package com.council.tenderapp.reg.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.models.Session;

class SessionServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void issuesUniqueTokensThatCanBeLookedUp() {
        SessionService service = new SessionService(Duration.ofHours(1), Clock.fixed(START, ZoneOffset.UTC));

        Session first = service.create(1L, Role.CUSTOMER);
        Session second = service.create(1L, Role.CUSTOMER);

        assertThat(first.token()).isNotEqualTo(second.token());
        assertThat(first.expiresAt()).isEqualTo(START.plus(Duration.ofHours(1)));
        assertThat(service.find(first.token())).contains(first);
    }

    @Test
    void invalidatedSessionIsGone() {
        SessionService service = new SessionService(Duration.ofHours(1), Clock.fixed(START, ZoneOffset.UTC));
        Session session = service.create(1L, Role.STAFF);

        service.invalidate(session.token());

        assertThat(service.find(session.token())).isEmpty();
    }

    @Test
    void expiredSessionIsNotFound() {
        MutableClock clock = new MutableClock(START);
        SessionService service = new SessionService(Duration.ofHours(1), clock);
        Session session = service.create(1L, Role.OPERATOR);

        clock.now = START.plus(Duration.ofMinutes(59));
        assertThat(service.find(session.token())).isPresent();

        clock.now = START.plus(Duration.ofHours(1));
        assertThat(service.find(session.token())).isEmpty();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
