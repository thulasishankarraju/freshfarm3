package com.example.freshfarm3.service;


import com.example.freshfarm3.dto.request.ProductRequest;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.ProductImage;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository    productRepository;
    private final FarmerRepository     farmerRepository;
    private final CategoryRepository   categoryRepository;
    private final FileUploadService    fileUploadService;

    // ── CREATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse createProduct(ProductRequest req,
                                         String farmerEmail,
                                         List<MultipartFile> images) {
        Farmer farmer = farmerRepository.findByUserEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .unit(req.getUnit())
                .stockQuantity(req.getStockQuantity())   // Sprint 3
                .isAvailable(req.getStockQuantity() > 0) // Sprint 3
                .grade(req.getGrade())
                .origin(req.getOrigin())
                .isOrganic(req.getIsOrganic())
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
        product.setUnit(req.getUnit());
        product.setGrade(req.getGrade());
        product.setOrigin(req.getOrigin());
        product.setIsOrganic(req.getIsOrganic());

        // Sprint 3 — update inventory fields
        product.setStockQuantity(req.getStockQuantity());
        product.setIsAvailable(req.getStockQuantity() > 0);

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
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
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllAvailableProducts(Pageable pageable) {
        return productRepository.findByIsAvailableTrue(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository
                .findByNameContainingIgnoreCaseAndIsAvailableTrue(keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> filterByCategory(Long categoryId, Pageable pageable) {
        return productRepository
                .findByCategoryIdAndIsAvailableTrue(categoryId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(String farmerEmail) {
        Farmer farmer = farmerRepository.findByUserEmail(farmerEmail)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));
        return productRepository.findByFarmer(farmer).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Sprint 3: Stock Validation ───────────────────────────────
    /**
     * Validates that the product exists, is available, and has enough stock.
     * Called by CartService before adding an item.
     */
    @Transactional(readOnly = true)
    public void validateStock(Long productId, int requestedQty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new RuntimeException("Product is currently unavailable: " + product.getName());
        }
        if (!product.hasStock(requestedQty)) {
            throw new RuntimeException(
                    "Insufficient stock for '" + product.getName() +
                            "'. Available: " + product.getStockQuantity() +
                            ", Requested: " + requestedQty
            );
        }
    }

    /**
     * Deducts stock after a confirmed order.
     * Called by OrderService inside a transaction.
     */
    @Transactional
    public void deductStock(Long productId, int qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.deductStock(qty);   // uses the entity helper — prevents negative stock
        productRepository.save(product);
        log.info("Stock deducted: productId={} qty={} remaining={}", productId, qty, product.getStockQuantity());
    }

    /**
     * Restores stock when an order is cancelled.
     * Called by OrderService.
     */
    @Transactional
    public void restoreStock(Long productId, int qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.restoreStock(qty);
        productRepository.save(product);
        log.info("Stock restored: productId={} qty={}", productId, qty);
    }
    // ─────────────────────────────────────────────────────────────

    // ── INTERNAL HELPERS ─────────────────────────────────────────
    private Product getProductOwnedByFarmer(Long productId, String farmerEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (!product.getFarmer().getUser().getEmail().equals(farmerEmail)) {
            throw new AccessDeniedException("You do not own this product");
        }
        return product;
    }

    public ProductResponse mapToResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setDescription(p.getDescription());
        res.setPrice(p.getPrice());
        res.setUnit(p.getUnit());
        res.setStockQuantity(p.getStockQuantity());   // Sprint 3
        res.setIsAvailable(p.getIsAvailable());        // Sprint 3
        res.setGrade(p.getGrade() != null ? p.getGrade().name() : null);
        res.setOrigin(p.getOrigin());
        res.setIsOrganic(p.getIsOrganic());
        res.setCategoryId(p.getCategory().getId());
        res.setCategoryName(p.getCategory().getName());
        res.setFarmerId(p.getFarmer().getId());
        res.setFarmerName(p.getFarmer().getUser().getName());
        res.setImages(p.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList()));
        return res;
    }
}