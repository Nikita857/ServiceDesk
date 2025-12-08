package com.bm.wschat.shared.repository;

import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserSelectableTrueOrderByDisplayOrderAsc();

    List<Category> findByTypeOrderByDisplayOrderAsc(CategoryType type);

    List<Category> findAllByOrderByDisplayOrderAsc();

    boolean existsByName(String name);
}
