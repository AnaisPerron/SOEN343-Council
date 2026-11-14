package com.council.tenderapp.operator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.council.tenderapp.operator.reset.ResetCoordinationFilter;
import com.council.tenderapp.operator.reset.ResetCoordinator;

class ResetCoordinationFilterTest {
    @ParameterizedTest
    @ValueSource(strings = {"/api/operator/reset", "/api/operator/reset-seed"})
    void bothResetRoutesAcquireExclusiveGuard(String path) throws Exception {
        var coordinator = mock(ResetCoordinator.class);
        var guard = mock(ResetCoordinator.Guard.class);
        when(coordinator.reset()).thenReturn(guard);
        var request = new MockHttpServletRequest("POST", "/tender" + path);
        request.setContextPath("/tender");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new ResetCoordinationFilter(coordinator).doFilter(request, response, chain);

        verify(coordinator).reset();
        verify(coordinator, never()).request();
        verify(chain).doFilter(request, response);
        verify(guard).close();
    }

    @Test
    void ordinaryRequestsAcquireSharedGuard() throws Exception {
        var coordinator = mock(ResetCoordinator.class);
        var guard = mock(ResetCoordinator.Guard.class);
        when(coordinator.request()).thenReturn(guard);
        var request = new MockHttpServletRequest("GET", "/api/operator/summary");

        new ResetCoordinationFilter(coordinator).doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(coordinator).request();
        verify(coordinator, never()).reset();
        verify(guard).close();
    }
}
