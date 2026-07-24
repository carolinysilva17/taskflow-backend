package com.carolinysilva.taskflow_backend.dto;

import com.carolinysilva.taskflow_backend.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String color
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
}
