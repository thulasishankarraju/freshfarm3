package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.ProductRequest;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    // ═══════════════════════════════════════════════
    //  PUBLIC ENDPOINTS — anonymous + buyer + farmer
    // ═══════════════════════════════════════════════

    /** GET /api/products → all available products (paginated) */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllAvailableProducts(pageable));
    }

    /** GET /api/products/{id} → single product */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /** GET /api/products/search?keyword=tomato */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String keyword, Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(keyword, pageable));
    }

    /** GET /api/products/category/{categoryId} */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> filterByCategory(
            @PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok(productService.filterByCategory(categoryId, pageable));
    }

    // ═══════════════════════════════════════════════
    //  FARMER-ONLY ENDPOINTS
    // ═══════════════════════════════════════════════

    /** POST /api/products — create product, optionally with images */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestPart("product") ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        String farmerEmail = currentUserEmail();
        ProductResponse response = productService.createProduct(request, farmerEmail, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/products/{id} — update product */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        String farmerEmail = currentUserEmail();
        return ResponseEntity.ok(productService.updateProduct(id, request, farmerEmail));
    }

    /** DELETE /api/products/{id} — delete product */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        String farmerEmail = currentUserEmail();
        productService.deleteProduct(id, farmerEmail);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    /** GET /api/products/my-products — farmer's own listings */
    @GetMapping("/my-products")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        String farmerEmail = currentUserEmail();
        return ResponseEntity.ok(productService.getMyProducts(farmerEmail));
    }

    // ── Private helper ────────────────────────────────────────

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}