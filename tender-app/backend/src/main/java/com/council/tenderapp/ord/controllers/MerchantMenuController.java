package com.council.tenderapp.ord.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.ord.dtos.MenuItemDto;
import com.council.tenderapp.ord.dtos.MenuItemRequestDto;
import com.council.tenderapp.ord.services.MerchantMenuService;
import com.council.tenderapp.reg.security.Session;
import com.council.tenderapp.reg.services.SessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/restaurant/menu-items")
public class MerchantMenuController {

    private final MerchantMenuService merchantMenuService;
    private final SessionService sessionService;

    public MerchantMenuController(MerchantMenuService merchantMenuService, SessionService sessionService) {
        this.merchantMenuService = merchantMenuService;
        this.sessionService = sessionService;
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<MenuItemDto> toggleAvailability(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long id
    ) {
        Long restaurantId = getValidatedRestaurantId(token);

        MenuItemDto updatedItem = merchantMenuService.toggleAvailability(restaurantId, id);
        
        return ResponseEntity.ok(updatedItem);
    }

    private Long getValidatedRestaurantId(String token) {
        Session session = sessionService.find(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session token"));
        
        if (session.restaurantId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Not a restaurant merchant account");
        }
        return session.restaurantId();
    }

    @GetMapping
    public ResponseEntity<List<MenuItemDto>> listMenuItems(@RequestHeader("X-Session-Token") String token) {
        Long restaurantId = getValidatedRestaurantId(token);
        List<MenuItemDto> items = merchantMenuService.listMenuItems(restaurantId);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<MenuItemDto> createMenuItem(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody MenuItemRequestDto request
    ) {
        getValidatedRestaurantId(token);
        MenuItemDto createdItem = merchantMenuService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemDto> updateMenuItem(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequestDto request
    ) {
        getValidatedRestaurantId(token);
        MenuItemDto updatedItem = merchantMenuService.updateMenuItem(id, request);
        return ResponseEntity.ok(updatedItem);
    }
}