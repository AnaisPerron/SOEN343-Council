package com.council.tenderapp.ord.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.council.tenderapp.ord.dtos.RestaurantBrowseProjection;
import com.council.tenderapp.ord.dtos.RestaurantResponseDTO;
import com.council.tenderapp.ord.repositories.MenuItemOptionRepository;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;

@Service 
public class BrowseService 
{
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;

    public BrowseService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository, MenuItemOptionRepository menuItemOptionRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuItemOptionRepository = menuItemOptionRepository;
    }

    public List<RestaurantResponseDTO> getOpenRestaurants() {
        
        List<RestaurantBrowseProjection> restaurants = restaurantRepository.findRestaurantBrowseByStatus("open");

        return restaurants.stream().map(p-> new RestaurantResponseDTO(
            p.getId(),
            p.getName(),
            p.getStatus(),
            p.getLatitude(),
            p.getLongitude(),
            p.getPhotoUrl()
        )
        ).toList();
    }

    /*public List<MenuResponseDTO> getMenuByRestaurantId(Long restaurantId) {

    }*/
}
