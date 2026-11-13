package com.council.tenderapp.operator.services;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
@PreAuthorize("hasRole('OPERATOR')")
public class DemoSeedResetService {
    private final OperatorResetService reset;

    public DemoSeedResetService(OperatorResetService reset) {
        this.reset = reset;
    }

    public void reset() {
        reset.reset();
    }
}
