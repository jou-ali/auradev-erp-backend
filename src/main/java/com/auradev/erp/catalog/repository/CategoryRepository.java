package com.auradev.erp.catalog.repository;

import com.auradev.erp.catalog.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderByNameAsc();

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT c FROM Category c
            WHERE c.active = true
              AND lower(c.name) LIKE lower(concat('%', :q, '%'))
            ORDER BY c.name ASC
            """)
    List<Category> searchActive(@Param("q") String q, Pageable pageable);
}
