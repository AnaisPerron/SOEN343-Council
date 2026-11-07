package com.council.tenderapp.ord.controllers;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.entities.MenuPhoto;
import com.council.tenderapp.ord.services.MerchantMenuService;

/** Serves uploaded menu photos publicly, like the seeded /menu-items-imgs files. */
@RestController
public class MenuPhotoController {

    private final MerchantMenuService merchantMenuService;

    public MenuPhotoController(MerchantMenuService merchantMenuService) {
        this.merchantMenuService = merchantMenuService;
    }

    @GetMapping("/menu-photos/{id}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id)
    {
        MenuPhoto photo = merchantMenuService.getPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                .body(photo.getData());
    }
}
