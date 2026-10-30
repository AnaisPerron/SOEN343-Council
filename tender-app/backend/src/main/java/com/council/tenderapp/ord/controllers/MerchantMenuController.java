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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        Long restaurantId = getValidatedRestaurantId(authHeader);

        MenuItemDto updatedItem = merchantMenuService.toggleAvailability(restaurantId, id);
        
        return ResponseEntity.ok(updatedItem);
    }

    private Long getValidatedRestaurantId( @RequestHeader(value = "Authorization", required = false) String authHeader)
    {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7); // Strip "Bearer "
        Session session = sessionService.find(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session token"));
        
        if (session.restaurantId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Not a restaurant merchant account");
        }
        return session.restaurantId();
    }

    @GetMapping
    public ResponseEntity<List<MenuItemDto>> listMenuItems(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long restaurantId = getValidatedRestaurantId(authHeader);
        List<MenuItemDto> items = merchantMenuService.listMenuItems(restaurantId);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<MenuItemDto> createMenuItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody MenuItemRequestDto request
    ) {
        getValidatedRestaurantId(authHeader);
        MenuItemDto createdItem = merchantMenuService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemDto> updateMenuItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequestDto request
    ) {
        getValidatedRestaurantId(authHeader);
        MenuItemDto updatedItem = merchantMenuService.updateMenuItem(id, request);
        return ResponseEntity.ok(updatedItem);
    }
}