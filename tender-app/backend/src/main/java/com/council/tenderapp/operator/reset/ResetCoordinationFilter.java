package com.council.tenderapp.operator.reset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

public class ResetCoordinationFilter extends OncePerRequestFilter {
    private final ResetCoordinator coordinator;

    public ResetCoordinationFilter(ResetCoordinator coordinator) { this.coordinator = coordinator; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
        boolean reset = "POST".equals(request.getMethod())
                && ((request.getContextPath() + "/api/operator/reset").equals(path)
                    || (request.getContextPath() + "/api/operator/reset-seed").equals(path));
        try (var guard = reset ? coordinator.reset() : coordinator.request()) {
            chain.doFilter(request, response);
        }
    }
}
