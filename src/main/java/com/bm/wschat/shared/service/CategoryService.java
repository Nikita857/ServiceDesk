package com.bm.wschat.shared.service;

import com.bm.wschat.shared.dto.CategoryResponse;
import com.bm.wschat.shared.dto.category.request.CreateCategoryRequest;
import com.bm.wschat.shared.dto.category.request.UpdateCategoryRequest;
import com.bm.wschat.shared.dto.category.response.CategoryDetailResponse;
import com.bm.wschat.shared.mapper.CategoryMapper;
import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.model.CategoryType;
import com.bm.wschat.shared.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryDetailResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new EntityExistsException("Категория с названием '" + request.name() + "' уже существует");
        }

        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDetailResponse(saved);
    }

    public CategoryDetailResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + id));
        return categoryMapper.toDetailResponse(category);
    }

    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        return categoryMapper.toResponses(categories);
    }

    public List<CategoryResponse> getUserSelectableCategories() {
        List<Category> categories = categoryRepository.findByUserSelectableTrueOrderByDisplayOrderAsc();
        return categoryMapper.toResponses(categories);
    }

    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        List<Category> categories = categoryRepository.findByTypeOrderByDisplayOrderAsc(type);
        return categoryMapper.toResponses(categories);
    }

    @Transactional
    public CategoryDetailResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + id));

        if (request.name() != null && !request.name().equals(category.getName())) {
            if (categoryRepository.existsByName(request.name())) {
                throw new EntityExistsException("Категория с названием '" + request.name() + "' уже существует");
            }
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.type() != null) {
            category.setType(request.type());
        }
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        if (request.userSelectable() != null) {
            category.setUserSelectable(request.userSelectable());
        }

        Category updated = categoryRepository.save(category);
        return categoryMapper.toDetailResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + id));
        categoryRepository.delete(category); // Soft delete via @SQLDelete
    }
}
