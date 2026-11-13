package com.council.tenderapp.operator.services;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

/** Replays the same demo-only seed script used when starting with the demo profile. */
@Service
@Profile("demo")
public class DemoSeedResetService {
    private final DataSource dataSource;

    public DemoSeedResetService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void reset() {
        new ResourceDatabasePopulator(new ClassPathResource("demo/seed.sql")).execute(dataSource);
    }
}
