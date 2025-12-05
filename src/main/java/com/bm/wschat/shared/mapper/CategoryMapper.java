package com.bm.wschat.shared.mapper;

import com.bm.wschat.shared.dto.CategoryResponse;
import com.bm.wschat.shared.dto.category.request.CreateCategoryRequest;
import com.bm.wschat.shared.dto.category.response.CategoryDetailResponse;
import com.bm.wschat.shared.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    Category toEntity(CreateCategoryRequest request);

    CategoryDetailResponse toDetailResponse(Category category);

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponses(List<Category> categories);

    List<CategoryDetailResponse> toDetailResponses(List<Category> categories);
}
