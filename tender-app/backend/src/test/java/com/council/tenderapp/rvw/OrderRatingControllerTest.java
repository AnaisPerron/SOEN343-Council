package com.council.tenderapp.rvw;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;
import com.council.tenderapp.rvw.controllers.OrderRatingController;
import com.council.tenderapp.rvw.dtos.OrderRatingResponseDto;
import com.council.tenderapp.rvw.dtos.SubmitRatingRequestDto;
import com.council.tenderapp.rvw.exceptions.InvalidRatingException;
import com.council.tenderapp.rvw.exceptions.OrderAlreadyRatedException;
import com.council.tenderapp.rvw.services.OrderRatingService;

// Real security chain and sessions; the rating service is mocked.
@WebMvcTest(controllers = OrderRatingController.class)
@Import({SecurityConfig.class, SessionService.class})
class OrderRatingControllerTest {
    private static final String RATE = "/api/orders/{orderId}/rating";
    private static final String BODY = """
            {"restaurantStars":5,"courierStars":4,"comment":"Hot and on time"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private SessionService sessionService;
    @MockitoBean private OrderRatingService ratings;

    private String bearer(Role role) {
        return "Bearer " + sessionService.create(7L, role).token();
    }

    private org.springframework.test.web.servlet.ResultActions rate(Role role, String body) throws Exception {
        return mockMvc.perform(post(RATE, 42L).header(HttpHeaders.AUTHORIZATION, bearer(role))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void customerRatesWithTheirOwnSessionIdAndGets201() throws Exception {
        when(ratings.rate(eq(7L), eq(42L), any())).thenReturn(new OrderRatingResponseDto(
                1L, 42L, 2L, 6L, 5, 4, "Hot and on time", Instant.parse("2026-11-25T12:00:00Z")));
        rate(Role.CUSTOMER, BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.restaurantStars").value(5))
                .andExpect(jsonPath("$.courierStars").value(4));
        verify(ratings).rate(7L, 42L, new SubmitRatingRequestDto(5, 4, "Hot and on time"));
    }

    @Test
    void unauthenticatedIs401AndOtherRolesAre403() throws Exception {
        mockMvc.perform(post(RATE, 42L).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.COURIER, Role.STAFF, Role.OPERATOR)) {
            rate(role, BODY).andExpect(status().isForbidden());
        }
        verifyNoInteractions(ratings);
    }

    @Test
    void starsOutsideOneToFiveOrMissingRestaurantStarsAre400() throws Exception {
        for (String body : List.of("{\"restaurantStars\":0}", "{\"restaurantStars\":6}",
                "{\"restaurantStars\":3,\"courierStars\":9}", "{\"courierStars\":3}")) {
            rate(Role.CUSTOMER, body).andExpect(status().isBadRequest());
        }
        rate(Role.CUSTOMER, "{\"restaurantStars\":3,\"comment\":\"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.comment").exists());
        verifyNoInteractions(ratings);
    }

    @Test
    void serviceRefusalsMapToHttpStatuses() throws Exception {
        when(ratings.rate(eq(7L), eq(42L), any())).thenThrow(new OrderAlreadyRatedException(42L));
        rate(Role.CUSTOMER, BODY).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order 42 has already been rated"));

        when(ratings.rate(eq(7L), eq(42L), any())).thenThrow(new InvalidOrderStateException("not delivered"));
        rate(Role.CUSTOMER, BODY).andExpect(status().isConflict());

        when(ratings.rate(eq(7L), eq(42L), any())).thenThrow(new OrderAccessDeniedException("not yours"));
        rate(Role.CUSTOMER, BODY).andExpect(status().isForbidden());

        when(ratings.rate(eq(7L), eq(42L), any())).thenThrow(new OrderNotFoundException("missing"));
        rate(Role.CUSTOMER, BODY).andExpect(status().isNotFound());

        when(ratings.rate(eq(7L), eq(42L), any())).thenThrow(new InvalidRatingException("no courier"));
        rate(Role.CUSTOMER, BODY).andExpect(status().isBadRequest());
    }
}
