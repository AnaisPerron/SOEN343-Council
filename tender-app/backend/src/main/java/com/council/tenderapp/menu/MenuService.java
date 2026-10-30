package com.council.tenderapp.menu;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.menu.MenuController.Item;
import com.council.tenderapp.menu.MenuController.MenuDocument;
import com.council.tenderapp.menu.MenuController.Option;

@Service
public class MenuService {
    private final JdbcTemplate jdbc;
    public MenuService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private long ownedRestaurant(long accountId) {
        List<Long> ids = jdbc.query("select restaurant_id from restaurant_staff where user_id = ?", (rs, n) -> rs.getLong(1), accountId);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No restaurant is assigned to this account");
        return ids.get(0);
    }

    @Transactional(readOnly = true)
    public MenuDocument get(long accountId) {
        long restaurantId = ownedRestaurant(accountId);
        var restaurants = jdbc.query("select name, status, address, delivery_fee, cover_url from restaurants where id = ?",
                (rs, n) -> new Object[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5)}, restaurantId);
        if (restaurants.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        Object[] restaurant = restaurants.get(0);
        // Menus no longer have a name column; each restaurant has a single menu.
        var menuIds = jdbc.query("select id from menus where restaurant_id = ? order by id", (rs, n) -> rs.getLong(1), restaurantId);
        List<Item> items = new ArrayList<>();
        for (Long menuId : menuIds) {
            items.addAll(jdbc.query("select id, name, description, category, price, status, photo_url from menu_items where menu_id = ? order by id",
                (rs, n) -> {
                    long itemId = rs.getLong(1);
                    List<Option> options = jdbc.query("select id, name, additional_cost from menu_item_options where menu_item_id = ? order by id",
                        (optionRs, index) -> new Option(optionRs.getLong(1), optionRs.getString(2), optionRs.getBigDecimal(3)), itemId);
                    return new Item(itemId, rs.getString(2), rs.getString(3) == null ? "" : rs.getString(3),
                        rs.getString(4) == null ? "Menu" : rs.getString(4), rs.getBigDecimal(5), rs.getString(6), rs.getString(7), options);
                }, menuId));
        }
        return new MenuDocument(restaurantId, (String) restaurant[0], (String) restaurant[1],
            restaurant[2] == null ? "" : (String) restaurant[2], restaurant[3] == null ? BigDecimal.ZERO : (BigDecimal) restaurant[3],
            (String) restaurant[4], "Menu", items);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        return value.trim();
    }

    private static BigDecimal price(BigDecimal value, String label) {
        if (value == null || value.signum() < 0 || value.scale() > 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be a nonnegative amount with at most two decimals");
        return value;
    }

    private long insert(String sql, Object... values) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            return statement;
        }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("Insert did not return an ID");
        return keys.getKey().longValue();
    }

    @Transactional
    public MenuDocument save(long accountId, MenuDocument document) {
        long restaurantId = ownedRestaurant(accountId);
        if (document == null || document.restaurantId() == null || document.restaurantId() != restaurantId)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This menu does not belong to your restaurant");
        String status = required(document.status(), "Restaurant status");
        if (!List.of("open", "paused", "closed").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid restaurant status");
        jdbc.update("update restaurants set name = ?, status = ?, address = ?, delivery_fee = ?, cover_url = ? where id = ?",
            required(document.name(), "Restaurant name"), status, document.address(), price(document.deliveryFee() == null ? BigDecimal.ZERO : document.deliveryFee(), "Delivery fee"), document.coverUrl(), restaurantId);
        var menuIds = jdbc.query("select id from menus where restaurant_id = ? order by id", (rs, n) -> rs.getLong(1), restaurantId);
        long menuId = menuIds.isEmpty() ? insert("insert into menus (restaurant_id) values (?)", restaurantId) : menuIds.get(0);
        if (document.items() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Items are required");
        for (Item item : document.items()) {
            String itemStatus = required(item.status(), "Item status");
            if (!List.of("available", "sold_out").contains(itemStatus)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item status");
            long itemId;
            if (item.id() == null) {
                itemId = insert("insert into menu_items (menu_id, name, price, status, photo_url, description, category) values (?, ?, ?, ?, ?, ?, ?)",
                    menuId, required(item.name(), "Item name"), price(item.price(), "Item price"), itemStatus, item.photoUrl(), item.description(), required(item.category(), "Category"));
            } else {
                int changed = jdbc.update("update menu_items set name = ?, price = ?, status = ?, photo_url = ?, description = ?, category = ? where id = ? and menu_id in (select id from menus where restaurant_id = ?)",
                    required(item.name(), "Item name"), price(item.price(), "Item price"), itemStatus, item.photoUrl(), item.description(), required(item.category(), "Category"), item.id(), restaurantId);
                if (changed == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Item does not belong to your restaurant");
                itemId = item.id();
            }
            if (item.options() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Options are required");
            for (Option option : item.options()) {
                if (option.id() == null) {
                    insert("insert into menu_item_options (menu_item_id, name, additional_cost) values (?, ?, ?)", itemId, required(option.name(), "Topping name"), price(option.additionalCost(), "Topping cost"));
                } else {
                    int changed = jdbc.update("update menu_item_options set name = ?, additional_cost = ? where id = ? and menu_item_id = ?",
                        required(option.name(), "Topping name"), price(option.additionalCost(), "Topping cost"), option.id(), itemId);
                    if (changed == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Topping does not belong to this item");
                }
            }
        }
        return get(accountId);
    }
}
