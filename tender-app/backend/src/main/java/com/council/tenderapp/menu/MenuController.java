package com.council.tenderapp.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/restaurant/menu")
@PreAuthorize("hasRole('STAFF')")
public class MenuController {
    private final MenuService service;

    public MenuController(MenuService service) { this.service = service; }

    public record Option(Long id, String name, BigDecimal additionalCost) {}
    public record Item(Long id, String name, String description, String category, BigDecimal price,
                       String status, String photoUrl, List<Option> options) {}
    public record MenuDocument(Long restaurantId, String name, String status, String address,
                               BigDecimal deliveryFee, String coverUrl, String menuName, List<Item> items) {}

    private long accountId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }

    @GetMapping
    public MenuDocument get(Authentication authentication) { return service.get(accountId(authentication)); }

    @PutMapping
    public MenuDocument save(Authentication authentication, @RequestBody MenuDocument document) {
        return service.save(accountId(authentication), document);
    }
}
