package com.council.tenderapp.technicalservices.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.AsyncEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class CommandLoggingFilterTest {
    private final CommandLoggingFilter filter = new CommandLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(CommandLoggingFilter.class);
    private ListAppender<ILoggingEvent> appender;
    private Level previous;

    @BeforeEach
    void setUp() {
        previous = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(previous);
        appender.stop();
    }

    @Test
    void logsActorAndOutcomeWithoutReadingCredentialsOrResponses() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Authorization", "Bearer secret-token");
        request.setQueryString("password=secret-query");
        request.setContent("{\"password\":\"secret-password\"}".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            assertThat(req.getReader().readLine()).contains("secret-password");
            CommandLoggingFilter.recordActor(request, 8L, "OPERATOR");
            response.setStatus(201);
            response.getWriter().write("secret-response-token");
        });
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(message()).contains("command request=", "method=POST", "path=/api/auth/login", "actor=8", "role=OPERATOR", "status=201", "durationMs=", "outcome=completed")
                .doesNotContain("secret", "password", "Authorization");
        assertThat(response.getContentAsString()).isEqualTo("secret-response-token");
    }

    @Test
    void recordsClientAndServerFailuresWithTheirActualStatuses() throws Exception {
        for (int code : List.of(400, 401, 403, 404, 409, 500)) {
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", "/api/operator/orders"), response,
                    (req, res) -> response.setStatus(code));
            ILoggingEvent entry = appender.list.get(appender.list.size() - 1);
            assertThat(entry.getLevel()).isEqualTo(code >= 500 ? Level.ERROR : Level.WARN);
            assertThat(entry.getFormattedMessage()).contains("status=" + code, "actor=anonymous");
        }
    }

    @Test
    void unexpectedExceptionsAreLoggedAndRethrownWithoutTheirMessages() {
        var request = new MockHttpServletRequest("POST", "/api/orders");
        RuntimeException failure = new IllegalStateException("secret-password secret-token");
        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { throw failure; }))
                .isSameAs(failure);
        assertThat(appender.list).hasSize(1);
        assertThat(message()).contains("status=500", "outcome=exception", "error=IllegalStateException").doesNotContain("secret");
        assertThat(appender.list.get(0).getThrowableProxy()).isNull();
    }

    @Test
    void preservesTheStatusAlreadySentWhenALaterExceptionOccurs() {
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest("POST", "/api/test"), response, (req, res) -> {
            response.setStatus(201);
            response.flushBuffer();
            throw new IllegalStateException("secret-token");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(message()).contains("status=201", "outcome=exception").doesNotContain("secret-token");
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void logsHealthButNotAssetsOrCorsPreflightRequests() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/menu-items-imgs/truffle.jpg"), new MockHttpServletResponse(), (req, res) -> {});
        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/orders"), new MockHttpServletResponse(), (req, res) -> {});
        assertThat(appender.list).isEmpty();
        filter.doFilter(new MockHttpServletRequest("GET", "/health"), new MockHttpServletResponse(), (req, res) -> {});
        assertThat(message()).contains("path=/health");
    }

    @Test
    void supportsContextPathsAndNeutralizesLogLineInjection() throws Exception {
        var request = new MockHttpServletRequest("GET", "/tender/api/test\nspoof");
        request.setContextPath("/tender");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        assertThat(message()).contains("path=/tender/api/test_spoof").doesNotContain("\n");
    }

    @Test
    void logsAsyncStartAndErrorsWithoutWaitingForTheStreamToClose() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/restaurants/1/orders/events");
        request.setAsyncSupported(true);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> request.startAsync(request, response));
        assertThat(message()).contains("outcome=async_started");
        var context = (MockAsyncContext) request.getAsyncContext();
        context.getListeners().get(0).onError(new AsyncEvent(context, new IllegalStateException("secret-token")));
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getFormattedMessage()).contains("outcome=async_error").doesNotContain("secret-token");
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void logsHandledAsyncFailuresAtCompletion() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/test");
        request.setAsyncSupported(true);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> request.startAsync(request, response));
        var context = (MockAsyncContext) request.getAsyncContext();
        response.setStatus(500);
        context.getListeners().get(0).onComplete(new AsyncEvent(context));
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(appender.list.get(1).getFormattedMessage()).contains("status=500", "outcome=async_completed");
    }

    @Test
    void logsAsyncTimeoutsAsErrors() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/test");
        request.setAsyncSupported(true);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> request.startAsync(request, response));
        var context = (MockAsyncContext) request.getAsyncContext();
        context.getListeners().get(0).onTimeout(new AsyncEvent(context));
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(appender.list.get(1).getFormattedMessage()).contains("outcome=async_timeout");
    }

    private String message() { return appender.list.get(0).getFormattedMessage(); }
}
