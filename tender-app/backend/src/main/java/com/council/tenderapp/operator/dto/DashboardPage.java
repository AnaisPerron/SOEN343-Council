package com.council.tenderapp.operator.dto;

import java.util.List;

public record DashboardPage<T>(List<T> items, long total, int page, int size) {
    public DashboardPage {
        items = List.copyOf(items);
    }
}
