package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.ProductRequest;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.ProductImage;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.repository.CategoryRepository;
import com.example.freshfarm3.repository.FarmerRepository;
import com.example.freshfarm3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository    productRepository;
    private final FarmerRepository     farmerRepository;
    private final CategoryRepository   categoryRepository;
    private final FileUploadService    fileUploadService;

    // TODO: ProductRequest has no `unit` field, but Product.unit is nullable=false.
    // Placeholder used until this is resolved — either add `unit` to ProductRequest,
    // or make the column nullable if unit tracking isn't needed at creation time.
    private static final String DEFAULT_UNIT_PLACEHOLDER = "unit";

    // ── CREATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse createProduct(ProductRequest req,
                                         String farmerEmail,
                                         List<MultipartFile> images) {
        Farmer farmer = farmerRepository.findByUser_Email(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        int quantity = req.getQuantity() != null ? req.getQuantity() : 0;

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .unit(DEFAULT_UNIT_PLACEHOLDER) // TODO: see note above
                .stockQuantity(quantity)
                .isAvailable(quantity > 0)
                .status(resolveStatus(req.getStatus()))
                .isOrganic(false) // TODO: ProductRequest has no isOrganic field yet
                .category(category)
                .farmer(farmer)
                .build();

        if (images != null && !images.isEmpty()) {
            images.forEach(file -> {
                String url = fileUploadService.uploadFile(file);
                ProductImage pi = new ProductImage();
                pi.setImageUrl(url);
                pi.setProduct(product);
                product.getImages().add(pi);
            });
        }

        Product saved = productRepository.save(product);
        log.info("Product created: {} by farmer: {}", saved.getId(), farmerEmail);
        return mapToResponse(saved);
    }

    // ── UPDATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse updateProduct(Long productId,
                                         ProductRequest req,
                                         String farmerEmail) {
        Product product = getProductOwnedByFarmer(productId, farmerEmail);

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());

        int quantity = req.getQuantity() != null ? req.getQuantity() : 0;
        product.setStockQuantity(quantity);
        product.setIsAvailable(quantity > 0);

        if (req.getStatus() != null) {
            product.setStatus(resolveStatus(req.getStatus()));
        }

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
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
    public Page<ProductResponse> getAllAvailableProducts(Pageable pageable) {
        return productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository
                .findByNameContainingIgnoreCaseAndStatus(keyword, Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Optional filterByCategory(Long categoryId, Pageable pageable) {
        return productRepository
                .findByCategory_IdAndStatus(categoryId, Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(String farmerEmail) {
        Farmer farmer = farmerRepository.findByUser_Email(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));
        return productRepository.findByFarmer(farmer).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Sprint 3: Stock Validation ───────────────────────────────
    @Transactional(readOnly = true)
    public void validateStock(Long productId, int requestedQty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new IllegalStateException("Product is currently unavailable: " + product.getName());
        }
        if (!product.hasStock(requestedQty)) {
            throw new IllegalStateException(
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

    private Product.ProductStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return Product.ProductStatus.ACTIVE;
        }
        try {
            return Product.ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status '" + status + "'. Must be one of: ACTIVE, INACTIVE, OUT_OF_STOCK");
        }
    }

    public ProductResponse mapToResponse(Product p) {
        List<String> imageUrls = p.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

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
                .primaryImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .imageUrls(imageUrls)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}