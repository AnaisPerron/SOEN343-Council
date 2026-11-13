package com.council.tenderapp.operator.reset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class ResetSeedLoader {
    private final JdbcTemplate jdbc;
    private final Resource resource;

    public ResetSeedLoader(JdbcTemplate jdbc,
            @Value("${app.operator.reset-seed:classpath:demo/seed.sql}") Resource resource) {
        this.jdbc = jdbc;
        this.resource = resource;
    }

    public void reload() {
        String sql;
        try {
            sql = resource.getContentAsString(StandardCharsets.UTF_8)
                    .replaceAll("(?im)^\\s*(?:BEGIN|COMMIT)\\s*;\\s*$", "");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read reset seed", exception);
        }
        if (sql.isBlank()) throw new IllegalStateException("Reset seed is empty");
        var script = new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if (connection.getAutoCommit()) throw new IllegalStateException("Reset requires an active transaction");
            ScriptUtils.executeSqlScript(connection, script);
            return null;
        });
    }
}
