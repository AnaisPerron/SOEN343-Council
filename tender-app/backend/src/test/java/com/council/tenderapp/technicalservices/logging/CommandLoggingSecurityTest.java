package com.council.tenderapp.technicalservices.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@WebMvcTest(controllers = CommandLoggingSecurityTest.Endpoints.class)
@Import({SecurityConfig.class, SessionService.class, CommandLoggingSecurityTest.Endpoints.class})
class CommandLoggingSecurityTest {
    @RestController
    static class Endpoints {
        @GetMapping("/api/operator/log-test")
        void read() {}
    }

    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    private final Logger logger = (Logger) LoggerFactory.getLogger(CommandLoggingFilter.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() { logger.detachAppender(appender); appender.stop(); }

    @Test
    void capturesAuthenticationBeforeTheSecurityContextIsCleared() throws Exception {
        String token = sessions.create(8L, Role.OPERATOR).token();
        mvc.perform(get("/api/operator/log-test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("actor=8", "role=OPERATOR", "status=200").doesNotContain(token);
    }

    @Test
    void includesRequestsRejectedByTheSecurityFilters() throws Exception {
        mvc.perform(get("/api/operator/log-test")).andExpect(status().isUnauthorized());
        String token = sessions.create(1L, Role.CUSTOMER).token();
        mvc.perform(get("/api/operator/log-test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("actor=anonymous", "status=401");
        assertThat(appender.list.get(1).getFormattedMessage()).contains("actor=1", "role=CUSTOMER", "status=403").doesNotContain(token);
    }
}
