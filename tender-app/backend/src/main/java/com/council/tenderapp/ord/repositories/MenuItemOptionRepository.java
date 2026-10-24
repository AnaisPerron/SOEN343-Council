package com.council.tenderapp.ord.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.ord.entities.MenuItemOption;

public interface MenuItemOptionRepository extends JpaRepository<MenuItemOption, Long> {
    List<MenuItemOption> findByMenuItemIdIn(List<Long> menuItemIds);
}
