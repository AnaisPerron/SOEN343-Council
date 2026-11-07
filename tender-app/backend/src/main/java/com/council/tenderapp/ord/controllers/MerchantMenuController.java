package com.council.tenderapp.ord.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.ord.dtos.MenuItemDto;
import com.council.tenderapp.ord.dtos.MenuItemRequestDto;
import com.council.tenderapp.ord.dtos.MenuPhotoUploadDto;
import com.council.tenderapp.ord.services.MerchantMenuService;
import com.council.tenderapp.reg.security.Session;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/restaurant/menu-items")
@PreAuthorize("hasRole('STAFF')")
public class MerchantMenuController {

    private final MerchantMenuService merchantMenuService;

    public MerchantMenuController(MerchantMenuService merchantMenuService) {
        this.merchantMenuService = merchantMenuService;
    }

    private Long getRestaurantId(Authentication authentication) 
    {
        Session session = (Session) authentication.getDetails();
        if (session == null || session.restaurantId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Not a restaurant merchant account");
        }
        return session.restaurantId();
    }

    @GetMapping
    public ResponseEntity<List<MenuItemDto>> listMenuItems(Authentication authentication) 
    {
        Long restaurantId = getRestaurantId(authentication);
        List<MenuItemDto> items = merchantMenuService.listMenuItems(restaurantId);
        return ResponseEntity.ok(items);
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<MenuItemDto> toggleAvailability(Authentication authentication, @PathVariable Long id) 
    {
        Long restaurantId = getRestaurantId(authentication);

        MenuItemDto updatedItem = merchantMenuService.toggleAvailability(restaurantId, id);
        
        return ResponseEntity.ok(updatedItem);
    }

    @PostMapping
    public ResponseEntity<MenuItemDto> createMenuItem(Authentication authentication, @Valid @RequestBody MenuItemRequestDto request) 
    {
        Long restaurantId = getRestaurantId(authentication);
        MenuItemDto createdItem = merchantMenuService.createMenuItem(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemDto> updateMenuItem(Authentication authentication, @PathVariable Long id, @Valid @RequestBody MenuItemRequestDto request) 
    {
        Long restaurantId = getRestaurantId(authentication);
        MenuItemDto updatedItem = merchantMenuService.updateMenuItem(restaurantId, id, request);
        return ResponseEntity.ok(updatedItem);
    }

    @PostMapping("/photos")
    public ResponseEntity<Map<String, String>> uploadPhoto(Authentication authentication, @Valid @RequestBody MenuPhotoUploadDto request)
    {
        Long restaurantId = getRestaurantId(authentication);
        String url = merchantMenuService.uploadPhoto(restaurantId, request.dataUri());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }

    /**@DeleteMapping("/{id}")
    public ResponseEntity<MenuItemDto> deleteMenuItem(Authentication authentication, @PathVariable Long id) 
    {
        Long restaurantId = getRestaurantId(authentication);
        MenuItemDto deletedItem = merchantMenuService.deleteMenuItem(restaurantId, id);
        return ResponseEntity.ok(deletedItem);
    }**/
}