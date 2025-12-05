package com.bm.wschat.shared.dto.category.request;

import com.bm.wschat.shared.model.CategoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 150, message = "Name must not exceed 150 characters") String name,

        @Size(max = 500, message = "Description must not exceed 500 characters") String description,

        CategoryType type,

        @Min(value = 0, message = "Display order must be non-negative") Integer displayOrder,

        Boolean userSelectable) {
}
