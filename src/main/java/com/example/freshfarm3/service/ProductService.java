package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.ProductRequest;
import com.example.freshfarm3.dto.response.CategoryResponse;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.ProductImage;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.exception.ValidationException;
import com.example.freshfarm3.repository.CategoryRepository;
import com.example.freshfarm3.repository.FarmerRepository;
import com.example.freshfarm3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository  productRepository;
    private final FarmerRepository   farmerRepository;
    private final CategoryRepository categoryRepository;
    private final FileUploadService  fileUploadService;

    // ── CREATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse createProduct(ProductRequest req, String farmerEmail, List<MultipartFile> images) {
        Farmer farmer = (Farmer) farmerRepository.findByUser_Email(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerEmail));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .unit(req.getUnit())
                .stockQuantity(req.getQuantity())
                .isAvailable(req.getQuantity() != null && req.getQuantity() > 0)
                .grade(req.getGrade())
                .origin(req.getOrigin())
                .isOrganic(Boolean.TRUE.equals(req.getIsOrganic()))
                .status(parseStatus(req.getStatus()))
                .category(category)
                .farmer(farmer)
                .build();

        Product saved = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            fileUploadService.uploadProductImages(images, saved);
            saved = productRepository.findById(saved.getId()).orElse(saved);
        }

        log.info("Product created: {} by farmer: {}", saved.getId(), farmerEmail);
        return mapToResponse(saved);
    }

    // ── UPDATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequestDto req, String farmerEmail) {
        Product product = getProductOwnedByFarmer(productId, farmerEmail);

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setUnit(req.getUnit());
        product.setGrade(req.getGrade());
        product.setOrigin(req.getOrigin());
        product.setIsOrganic(Boolean.TRUE.equals(req.getIsOrganic()));
        product.setStockQuantity(req.getQuantity());
        product.setIsAvailable(req.getQuantity() != null && req.getQuantity() > 0);

        if (req.getStatus() != null) {
            product.setStatus(parseStatus(req.getStatus()));
        }
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
            product.setCategory(category);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated: {} by farmer: {}", productId, farmerEmail);
        return mapToResponse(updated);
    }

    // ── DELETE ───────────────────────────────────────────────────
    @Transactional
    public void deleteProduct(Long productId, String farmerEmail) {
        Product product = getProductOwnedByFarmer(productId, farmerEmail);
        productRepository.delete(product);
        log.info("Product deleted: {} by farmer: {}", productId, farmerEmail);
    }

    // ── READ ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByStatus(Product.ProductStatus.ACTIVE)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchByKeyword(keyword, Product.ProductStatus.ACTIVE)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> filterByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        return productRepository.findByCategoryAndStatus(category, Product.ProductStatus.ACTIVE)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchByCategoryAndKeyword(Long categoryId, String keyword) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        return productRepository.searchByCategoryAndKeyword(category, keyword, Product.ProductStatus.ACTIVE)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(String farmerEmail) {
        Farmer farmer = farmerRepository.findByUser_Email(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerEmail));
        return productRepository.findByFarmer(farmer).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── CATEGORIES ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(String name, String description) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ValidationException("Category already exists: " + name);
        }
        Category category = Category.builder().name(name).description(description).build();
        Category saved = categoryRepository.save(category);
        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    // ── Sprint 3: Stock Validation ───────────────────────────────
    @Transactional(readOnly = true)
    public void validateStock(Long productId, int requestedQty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new ValidationException("Product is currently unavailable: " + product.getName());
        }
        if (!product.hasStock(requestedQty)) {
            throw new ValidationException(
                    "Insufficient stock for '" + product.getName() +
                            "'. Available: " + product.getStockQuantity() +
                            ", Requested: " + requestedQty
            );
        }
    }

    @Transactional
    public void deductStock(Long productId, int qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.deductStock(qty);
        productRepository.save(product);
        log.info("Stock deducted: productId={} qty={} remaining={}", productId, qty, product.getStockQuantity());
    }

    @Transactional
    public void restoreStock(Long productId, int qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.restoreStock(qty);
        productRepository.save(product);
        log.info("Stock restored: productId={} qty={}", productId, qty);
    }

    // ── INTERNAL HELPERS ─────────────────────────────────────────
    private Product getProductOwnedByFarmer(Long productId, String farmerEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!product.getFarmer().getUser().getEmail().equals(farmerEmail)) {
            throw new AccessDeniedException("You do not own this product");
        }
        return product;
    }

    private Product.ProductStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Product.ProductStatus.ACTIVE;
        }
        try {
            return Product.ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid product status: " + status);
        }
    }

    public ProductResponse mapToResponse(Product p) {
        String primaryImageUrl = p.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> p.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .quantity(p.getStockQuantity())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .farmerId(p.getFarmer().getId())
                .farmerName(p.getFarmer().getUser().getFullName())
                .farmName(p.getFarmer().getFarmName())
                .primaryImageUrl(primaryImageUrl)
                .imageUrls(p.getImages().stream().map(ProductImage::getImageUrl).collect(Collectors.toList()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
