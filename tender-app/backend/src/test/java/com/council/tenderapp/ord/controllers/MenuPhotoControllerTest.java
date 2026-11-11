package com.council.tenderapp.ord.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MenuPhotoControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;

    private String staffToken() {
        registrationService.register(new RegisterRequest("Photo Bistro", "photo-bistro@example.com", "secret123",
                "514-555-0198", Role.STAFF, "123 Main St, Montreal, Canada", 45.50, -73.57));
        return loginService.login(new LoginRequest("photo-bistro@example.com", "secret123")).token();
    }

    @Test
    void uploadedPhotoIsServedWithoutSession() throws Exception {
        byte[] image = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        String body = mockMvc.perform(post("/api/restaurant/menu-items/photos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataUri\":\"data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(body, "$.url");

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(image));
    }

    @Test
    void rejectsNonImageUpload() throws Exception {
        mockMvc.perform(post("/api/restaurant/menu-items/photos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataUri\":\"data:text/html;base64,PGgxPg==\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRequiresSession() throws Exception {
        mockMvc.perform(post("/api/restaurant/menu-items/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataUri\":\"data:image/jpeg;base64,/9j/\"}"))
                .andExpect(status().isUnauthorized());
    }
}
