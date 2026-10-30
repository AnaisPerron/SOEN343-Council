package com.council.tenderapp.menu;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MenuPhotoController {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final List<String> TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final JdbcTemplate jdbc;
    private final MenuService menuService;

    public MenuPhotoController(JdbcTemplate jdbc, MenuService menuService) {
        this.jdbc = jdbc;
        this.menuService = menuService;
    }

    public record Upload(String dataUri) {}

    /** Stores a data URI (data:image/jpeg;base64,...) and returns the path to put in a menu item's photoUrl. */
    @PostMapping("/api/restaurant/menu/photos")
    @PreAuthorize("hasRole('STAFF')")
    public Map<String, String> upload(Authentication authentication, @RequestBody Upload upload) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long accountId))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String dataUri = upload == null ? null : upload.dataUri();
        int comma = dataUri == null ? -1 : dataUri.indexOf(',');
        if (comma < 0 || !dataUri.startsWith("data:") || !dataUri.substring(0, comma).endsWith(";base64"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a base64 data URI");
        String contentType = dataUri.substring(5, comma - ";base64".length());
        if (!TYPES.contains(contentType)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a JPEG, PNG, WebP or GIF image");
        byte[] data;
        try { data = Base64.getDecoder().decode(dataUri.substring(comma + 1)); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo data is not valid base64"); }
        if (data.length == 0 || data.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be under 5 MB");
        long id = menuService.insertPhoto(accountId, contentType, data);
        return Map.of("url", "/menu-photos/" + id);
    }

    @GetMapping("/menu-photos/{id}")
    public ResponseEntity<byte[]> get(@PathVariable long id) {
        var photos = jdbc.query("select content_type, data from menu_photos where id = ?",
            (rs, n) -> ResponseEntity.ok().contentType(MediaType.parseMediaType(rs.getString(1)))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic()).body(rs.getBytes(2)), id);
        if (photos.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return photos.get(0);
    }
}
