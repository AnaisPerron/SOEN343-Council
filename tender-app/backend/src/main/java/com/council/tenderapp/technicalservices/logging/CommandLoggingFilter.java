package com.council.tenderapp.technicalservices.logging;

import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommandLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CommandLoggingFilter.class);
    private static final String ACTOR = CommandLoggingFilter.class.getName() + ".actor";
    private record Actor(Long accountId, String role) {}

    public static void recordActor(HttpServletRequest request, Long accountId, String role) {
        request.setAttribute(ACTOR, new Actor(accountId, role));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod())
                || !(path.equals("/api") || path.startsWith("/api/") || path.equals("/health"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            write(request, response, requestId, start, "exception", failure, true);
            throw failure;
        }
        write(request, response, requestId, start, request.isAsyncStarted() ? "async_started" : "completed", null, false);
        if (request.isAsyncStarted()) {
            AsyncListener listener = new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    if (response.getStatus() >= 400) write(request, response, requestId, start, "async_completed", null, false);
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    write(request, response, requestId, start, "async_timeout", null, true);
                }

                @Override
                public void onError(AsyncEvent event) {
                    write(request, response, requestId, start, "async_error", event.getThrowable(), true);
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                    event.getAsyncContext().addListener(this);
                }
            };
            try {
                request.getAsyncContext().addListener(listener);
            } catch (IllegalStateException completed) {
                // async request already completed
            }
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response, String requestId,
            long start, String outcome, Throwable failure, boolean forceError) {
        Object value = request.getAttribute(ACTOR);
        Actor actor = value instanceof Actor found ? found : new Actor(null, "ANONYMOUS");
        int status = failure != null && !request.isAsyncStarted() && !response.isCommitted()
                ? Math.max(500, response.getStatus()) : response.getStatus();
        String message = "command request=" + requestId
                + " method=" + safe(request.getMethod(), 16)
                + " path=" + safe(request.getRequestURI(), 200)
                + " actor=" + (actor.accountId() == null ? "anonymous" : actor.accountId())
                + " role=" + safe(actor.role(), 30)
                + " status=" + status
                + " durationMs=" + (System.nanoTime() - start) / 1_000_000
                + " outcome=" + outcome
                + " error=" + (failure == null ? "none" : failure.getClass().getSimpleName());
        if (forceError || status >= 500) logger.error(message);
        else if (status >= 400) logger.warn(message);
        else logger.info(message);
    }

    private static String safe(String value, int limit) {
        String clean = value == null ? "unknown" : value.replaceAll("[\\p{Cntrl}\\s]", "_");
        return clean.substring(0, Math.min(clean.length(), limit));
    }
}
