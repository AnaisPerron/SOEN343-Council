package com.council.tenderapp.dash.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.dash.dtos.MapDataDto;
import com.council.tenderapp.dash.services.MapService;

/** Open to any logged-in account; the security filter chain refuses requests without a session. */
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping
    public ResponseEntity<MapDataDto> getMapData() {
        return ResponseEntity.ok(mapService.getMapData());
    }
}
