package com.bm.wschat.shared.dto.category.response;

import com.bm.wschat.shared.model.CategoryType;

import java.time.Instant;

public record CategoryDetailResponse(
        Long id,
        String name,
        String description,
        CategoryType type,
        Integer displayOrder,
        boolean userSelectable,
        Instant createdAt,
        Instant updatedAt) {
}
