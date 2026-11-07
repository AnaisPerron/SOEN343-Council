package com.council.tenderapp.ord.services;

import com.council.tenderapp.ord.dtos.MenuItemDto;
import com.council.tenderapp.ord.dtos.MenuItemRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.MenuPhoto;
import com.council.tenderapp.ord.exceptions.UnauthorizedMenuAccessException;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.MenuPhotoRepository;

import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service // 1. Crucial so Spring recognizes this service
public class MerchantMenuService {

    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;
    private static final List<String> PHOTO_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final MenuItemRepository menuItemRepository;
    private final MenuPhotoRepository menuPhotoRepository;

    public MerchantMenuService(MenuItemRepository menuItemRepository, MenuPhotoRepository menuPhotoRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuPhotoRepository = menuPhotoRepository;
    }

    @Transactional 
    public MenuItemDto toggleAvailability(Long restaurantId, Long menuItemId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found with id: " + menuItemId));

        if (!menuItemRepository.existsByIdAndRestaurantId(menuItemId, restaurantId)) {
            throw new UnauthorizedMenuAccessException("Menu item belongs to another restaurant");
        }

        menuItem.setStatus("sold_out".equals(menuItem.getStatus()) ? "available" : "sold_out");
        MenuItem savedItem = menuItemRepository.save(menuItem);

        return new MenuItemDto(savedItem);
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> listMenuItems(Long restaurantId) {
        return menuItemRepository.findMenuItemsByRestaurantId(restaurantId)
                .stream()
                .map(MenuItemDto::new)
                .toList();
    }

    @Transactional
    public MenuItemDto createMenuItem(Long restaurantId, MenuItemRequestDto request) {
        MenuItem menuItem = new MenuItem();
        Long menuId = menuItemRepository.findMenuIdByRestaurantId(restaurantId);
        if (menuId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant has no menu");
        menuItem.setMenuId(menuId);
        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setStatus(request.available() ? "available" : "sold_out");
        menuItem.setCategory(request.category());
        menuItem.setPhotoUrl(request.photoUrl());

        MenuItem saved = menuItemRepository.save(menuItem);
        return new MenuItemDto(saved);
    }

    @Transactional
    public MenuItemDto updateMenuItem(Long restaurantId, Long menuItemId, MenuItemRequestDto request) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found with id: " + menuItemId));

        boolean isOwner = menuItemRepository.existsByIdAndRestaurantId(menuItemId, restaurantId);
        if (!isOwner) 
        {
            throw new UnauthorizedMenuAccessException("Access denied: Menu item "+menuItemId+"- "+menuItem.getName()+" belongs to another restaurant");
        }

        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setStatus(request.available() ? "available" : "sold_out");
        menuItem.setCategory(request.category());
        menuItem.setPhotoUrl(request.photoUrl());


        MenuItem saved = menuItemRepository.save(menuItem);
        return new MenuItemDto(saved);
    }

    /** Stores a data URI (data:image/jpeg;base64,...) and returns the path to put in a menu item's photoUrl. */
    @Transactional
    public String uploadPhoto(Long restaurantId, String dataUri) {
        int comma = dataUri.indexOf(',');
        if (comma < 0 || !dataUri.startsWith("data:") || !dataUri.substring(0, comma).endsWith(";base64"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a base64 data URI");

        String contentType = dataUri.substring("data:".length(), comma - ";base64".length());
        if (!PHOTO_TYPES.contains(contentType))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a JPEG, PNG, WebP or GIF image");

        byte[] data;
        try {
            data = Base64.getDecoder().decode(dataUri.substring(comma + 1));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo data is not valid base64");
        }
        if (data.length == 0 || data.length > MAX_PHOTO_BYTES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be under 5 MB");

        MenuPhoto saved = menuPhotoRepository.save(new MenuPhoto(restaurantId, contentType, data));
        return "/menu-photos/" + saved.getId();
    }

    @Transactional(readOnly = true)
    public MenuPhoto getPhoto(Long photoId) {
        return menuPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found with id: " + photoId));
    }

    /**@Transactional
    public MenuItemDto deleteMenuItem(Long restaurantId, Long menuItemId)
    {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found with id: " + menuItemId));

        boolean isOwner = menuItemRepository.existsByIdAndRestaurantId(menuItemId, restaurantId);
        if (!isOwner) 
        {
            throw new UnauthorizedMenuAccessException("Menu item "+menuItemId+"- "+menuItem.getName()+" belongs to another restaurant");
        }

        menuItemRepository.deleteById(menuItemId);

        return new MenuItemDto(menuItem);
    }**/
}
