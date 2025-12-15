package com.bm.wschat.shared.dto.category.request;

import com.bm.wschat.shared.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required") @Size(max = 150, message = "Name must not exceed 150 characters") String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов") String description,

        CategoryType type,

        @Min(value = 0, message = "Порядок не должен быть отрицательным") Integer displayOrder,

        Boolean userSelectable) {
    public CreateCategoryRequest {
        if (type == null) {
            type = CategoryType.GENERAL;
        }
        if (displayOrder == null) {
            displayOrder = 100;
        }
        if (userSelectable == null) {
            userSelectable = true;
        }
    }
}
