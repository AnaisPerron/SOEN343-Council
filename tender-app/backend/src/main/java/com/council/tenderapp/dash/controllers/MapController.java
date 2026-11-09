package com.council.tenderapp.dash.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.dash.dtos.MapDataDto;
import com.council.tenderapp.dash.services.MapService;
import com.council.tenderapp.reg.security.Session;

/** Open to any logged-in account; what the map shows depends on the viewer's role (see MapService). */
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping
    public ResponseEntity<MapDataDto> getMapData(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Session session)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(mapService.getMapData(session));
    }
}
