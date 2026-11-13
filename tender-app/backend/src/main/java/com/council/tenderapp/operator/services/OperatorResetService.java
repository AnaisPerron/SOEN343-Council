package com.council.tenderapp.operator.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.operator.reset.ResetCoordinator;
import com.council.tenderapp.operator.reset.ResetSeedLoader;
import com.council.tenderapp.ord.observers.DashboardObserver;
import com.council.tenderapp.reg.services.SessionService;

@Service
@PreAuthorize("hasRole('OPERATOR')")
public class OperatorResetService {
    private final ResetCoordinator coordinator;
    private final ResetSeedLoader seed;
    private final SessionService sessions;
    private final DashboardObserver dashboards;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public OperatorResetService(ResetCoordinator coordinator, ResetSeedLoader seed, SessionService sessions,
            DashboardObserver dashboards, PlatformTransactionManager transactions,
            @Value("${app.operator.reset-enabled:false}") boolean enabled) {
        this.coordinator = coordinator;
        this.seed = seed;
        this.sessions = sessions;
        this.dashboards = dashboards;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.enabled = enabled;
    }

    public void reset() {
        if (!enabled) throw new ResponseStatusException(HttpStatus.CONFLICT, "System reset is disabled");
        try (var guard = coordinator.reset()) {
            transaction.executeWithoutResult(status -> seed.reload());
            sessions.invalidateAll();
            dashboards.completeAll();
        }
    }
}
