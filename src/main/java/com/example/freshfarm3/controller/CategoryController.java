package com.example.freshfarm3.controller;

import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /** GET /api/categories — list all categories (id + name + description) */
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}