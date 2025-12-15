package com.bm.wschat.shared.dto.category.request;

import com.bm.wschat.shared.model.CategoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 150, message = "Название не должно превышать 150 символов") String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов") String description,

        CategoryType type,

        @Min(value = 0, message = "Порядок не должен быть отрицательным") Integer displayOrder,

        Boolean userSelectable) {
}
