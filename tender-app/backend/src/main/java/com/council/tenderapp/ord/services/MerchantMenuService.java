package com.council.tenderapp.ord.services;

import com.council.tenderapp.ord.dtos.MenuItemDto;
import com.council.tenderapp.ord.dtos.MenuItemRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.exceptions.UnauthorizedMenuAccessException;
import com.council.tenderapp.ord.repositories.MenuItemRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service // 1. Crucial so Spring recognizes this service
public class MerchantMenuService {

    private final MenuItemRepository menuItemRepository;

    public MerchantMenuService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional 
    public MenuItemDto toggleAvailability(Long restaurantId, Long menuItemId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found with id: " + menuItemId));

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
        menuItem.setMenuId(restaurantId);
        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setStatus(request.available() ? "available" : "sold-out");

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
        menuItem.setStatus("sold_out".equals(menuItem.getStatus()) ? "available" : "sold_out");


        MenuItem saved = menuItemRepository.save(menuItem);
        return new MenuItemDto(saved);
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