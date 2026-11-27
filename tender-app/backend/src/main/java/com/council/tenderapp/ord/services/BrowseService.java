package com.council.tenderapp.ord.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.council.tenderapp.ord.dtos.MenuItemDto;
import com.council.tenderapp.ord.dtos.MenuItemOptionDto;
import com.council.tenderapp.ord.dtos.MenuResponseDTO;
import com.council.tenderapp.ord.dtos.RestaurantBrowseProjection;
import com.council.tenderapp.ord.dtos.RestaurantResponseDTO;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.MenuItemOption;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.exceptions.RestaurantClosedException;
import com.council.tenderapp.ord.exceptions.RestaurantNotFoundException;
import com.council.tenderapp.ord.repositories.MenuItemOptionRepository;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.rvw.dtos.RestaurantRatingSummary;
import com.council.tenderapp.rvw.services.RestaurantRatingService;

@Service 
public class BrowseService 
{
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;
    private final RestaurantRatingService restaurantRatings;

    public BrowseService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository,
            MenuItemOptionRepository menuItemOptionRepository, RestaurantRatingService restaurantRatings) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuItemOptionRepository = menuItemOptionRepository;
        this.restaurantRatings = restaurantRatings;
    }

    public List<RestaurantResponseDTO> getOpenRestaurants() {
        
        List<RestaurantBrowseProjection> restaurants = restaurantRepository.findRestaurantBrowseByStatus("open");
        // One grouped query for every listed restaurant instead of one per restaurant.
        Map<Long, RestaurantRatingSummary> ratings = restaurantRatings.summaries(
            restaurants.stream().map(RestaurantBrowseProjection::getId).toList());

        return restaurants.stream().map(p-> {
            RestaurantRatingSummary rating = ratings.getOrDefault(p.getId(), RestaurantRatingSummary.NONE);
            return new RestaurantResponseDTO(
                p.getId(),
                p.getName(),
                p.getStatus(),
                p.getLatitude(),
                p.getLongitude(),
                p.getPhotoUrl(),
                rating.averageRating(),
                rating.ratingCount()
            );
        }).toList();
    }

    public MenuResponseDTO getRestaurantMenu(Long restaurantId) {
        List<MenuItem> menuItems = menuItemRepository.findMenuItemsByRestaurantId(restaurantId);
        List<MenuItemOption> menuItemOptions = List.of();

        if(!menuItems.isEmpty()) {
            List<Long> menuItemIds = menuItems.stream().map(MenuItem::getId).toList();
            menuItemOptions = menuItemOptionRepository.findByMenuItemIdIn(menuItemIds);
        }

        //converting to DTOs

        List<MenuItemDto> menuItemDtos = menuItems.stream()
        .map(i -> new MenuItemDto(
            i.getId(), 
            i.getMenuId(), 
            i.getName(), 
            i.getPrice(), 
            i.getDescription(), 
            "available".equalsIgnoreCase(i.getStatus()), 
            i.getPhotoUrl(),
            i.getCategory()
        ))
        .toList();

        List<MenuItemOptionDto> menuItemOptionDtos = menuItemOptions.stream()
        .map(o -> new MenuItemOptionDto(
            o.getId(), 
            o.getMenuItemId(), 
            o.getName(), 
            o.getAdditionalCost()
        ))
        .toList();
        
        return new MenuResponseDTO(menuItemDtos, menuItemOptionDtos);
    }

    public void checkRestaurantOpen(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found with id"+ restaurantId));
    
            if(!"open".equalsIgnoreCase(restaurant.getStatus()))
                throw new RestaurantClosedException("Restaurant" + restaurant.getId() + " : " + restaurant.getName() + " is not open");
    }
}
