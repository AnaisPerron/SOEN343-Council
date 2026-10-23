package com.council.tenderapp.reg.services;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.security.Session;

// Sessions are kept in memory, so a backend restart signs everyone out
@Service
public class SessionService {

    private static final int TOKEN_BYTES = 32;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public SessionService(@Value("${app.auth.session-ttl:PT24H}") Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    SessionService(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public Session create(Long accountId, Role role) {
        Instant now = clock.instant();
        sessions.values().removeIf(session -> session.isExpiredAt(now));

        Session session = new Session(newToken(), accountId, role, now.plus(ttl));
        sessions.put(session.token(), session);
        return session;
    }

    public Optional<Session> find(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpiredAt(clock.instant())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void invalidate(String token) {
        sessions.remove(token);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
