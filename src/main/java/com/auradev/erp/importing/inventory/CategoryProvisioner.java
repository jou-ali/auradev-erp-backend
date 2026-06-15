package com.auradev.erp.importing.inventory;

import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Resolves categories by name, creating missing ones during import. */
@Component
@RequiredArgsConstructor
public class CategoryProvisioner {

    private final CategoryRepository categoryRepository;

    public Category resolveOrCreate(String name) {
        String trimmed = name.trim();
        return categoryRepository.findByNameIgnoreCase(trimmed)
                .or(() -> categoryRepository.findBySlug(slugify(trimmed)))
                .orElseGet(() -> createCategory(trimmed));
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(uniqueSlug(name));
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private String slugify(String name) {
        String base = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return base.isBlank() ? "category" : base;
    }

    private String uniqueSlug(String name) {
        String base = slugify(name);
        String slug = base;
        int n = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }
}
