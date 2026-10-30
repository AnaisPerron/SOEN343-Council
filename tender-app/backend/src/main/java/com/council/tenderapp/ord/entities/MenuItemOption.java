package com.council.tenderapp.ord.entities;
import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_item_options")
public class MenuItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "additional_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalCost;

    @Column(length = 150)
    private String name;

    protected MenuItemOption() {
    }

    public MenuItemOption(Long id, Long menuItemId, BigDecimal additionalCost, String name) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.additionalCost = additionalCost;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public BigDecimal getAdditionalCost() {
        return additionalCost;
    }

    public void setAdditionalCost(BigDecimal additionalCost) {
        this.additionalCost = additionalCost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
