package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.CategoryRequest;
import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /** GET /api/categories — list all categories (id + name + description). Open to everyone (shop/admin need it to build product forms, buyers need it to filter the store). */
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * POST /api/categories — admin-only. Shops can never add their own
     * categories (which is why the dropdown can be empty on a fresh
     * install) — an admin adds the categories products get filed under.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalStateException("A category named '" + request.getName() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Category saved = categoryRepository.save(category);
        log.info("Category created by admin: {}", saved.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** PUT /api/categories/{id} — admin-only: rename/edit a category. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    /** DELETE /api/categories/{id} — admin-only. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        categoryRepository.delete(category);
        return ResponseEntity.ok(Map.of("message", "Category deleted"));
    }
}
