package com.council.tenderapp.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import com.council.tenderapp.operator.reset.ResetCoordinator;

class ResetCoordinatorTest {
    @Test
    void resetWaitsForAnActiveRequest() throws Exception {
        var coordinator = new ResetCoordinator();
        var executor = Executors.newSingleThreadExecutor();
        var attempted = new CountDownLatch(1);
        try {
            java.util.concurrent.Future<Boolean> reset;
            try (var request = coordinator.request()) {
                reset = executor.submit(() -> {
                    attempted.countDown();
                    try (var exclusive = coordinator.reset()) { return true; }
                });
                assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> reset.get(100, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            }
            assertThat(reset.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void requestsWaitForResetToFinish() throws Exception {
        var coordinator = new ResetCoordinator();
        var executor = Executors.newSingleThreadExecutor();
        var attempted = new CountDownLatch(1);
        try {
            java.util.concurrent.Future<Boolean> request;
            try (var reset = coordinator.reset()) {
                request = executor.submit(() -> {
                    attempted.countDown();
                    try (var shared = coordinator.request()) { return true; }
                });
                assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> request.get(100, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            }
            assertThat(request.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
