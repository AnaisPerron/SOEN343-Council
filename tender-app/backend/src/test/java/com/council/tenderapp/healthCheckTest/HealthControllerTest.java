package com.council.tenderapp.healthCheckTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnHealthStatus() {

         RestClient client = RestClient.create();

        String response = client.get()
            .uri("http://localhost:" + port + "/health")
            .retrieve()
            .body(String.class);

        assertThat(response).contains("OK");
    }
}