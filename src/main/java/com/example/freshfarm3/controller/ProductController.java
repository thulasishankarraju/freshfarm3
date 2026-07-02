package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.ProductRequest;
import com.example.freshfarm3.dto.response.CategoryResponseDto;
import com.example.freshfarm3.dto.response.ProductResponseDto;
import com.example.freshfarm3.service.FileUploadService;
import com.example.freshfarm3.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService     productService;
    private final FileUploadService  fileUploadService;

    // ═══════════════════════════════════════════════
    //  PUBLIC ENDPOINTS — anonymous + buyer + farmer
    // ═══════════════════════════════════════════════

    /** GET /api/products → all active products */
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    /** GET /api/products/{id} → single product */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /** GET /api/products/search?keyword=tomato */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> searchProducts(
            @RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    /** GET /api/products/category/{categoryId} */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDto>> filterByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.filterByCategory(categoryId));
    }

    /** GET /api/products/category/{categoryId}/search?keyword=organic */
    @GetMapping("/category/{categoryId}/search")
    public ResponseEntity<List<ProductResponseDto>> searchByCategoryAndKeyword(
            @PathVariable Long categoryId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(
                productService.searchByCategoryAndKeyword(categoryId, keyword));
    }

    // ═══════════════════════════════════════════════
    //  CATEGORY ENDPOINTS — public read, farmer write
    // ═══════════════════════════════════════════════

    /** GET /api/products/categories */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    /** POST /api/products/categories — FARMER only */
    @PostMapping("/categories")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<CategoryResponseDto> createCategory(
            @RequestBody Map<String, String> body) {
        CategoryResponseDto response = productService.createCategory(
                body.get("name"), body.get("description"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ═══════════════════════════════════════════════
    //  FARMER-ONLY ENDPOINTS
    // ═══════════════════════════════════════════════

    /** POST /api/products — create product (JSON only, no image) */
    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** POST /api/products/with-image — create product + upload image */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProductResponseDto> createProductWithImage(
            @Valid @RequestPart("product") ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
            throws IOException {

        ProductResponseDto created = productService.createProduct(request);

        if (images != null && !images.isEmpty()) {
            com.example.freshfarm3.entity.Product product =
                    new com.example.freshfarm3.entity.Product();
            product.setId(created.getId());
            fileUploadService.uploadProductImages(images, product);

            // Re-fetch with images
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(productService.getProductById(created.getId()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/products/{id} — update product */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /** DELETE /api/products/{id} — delete product */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    /** GET /api/products/my-products — farmer's own listings */
    @GetMapping("/my-products")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<ProductResponseDto>> getMyProducts() {
        return ResponseEntity.ok(productService.getMyProducts());
    }
}
