package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.ProductRequest;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.ProductImage;
import com.example.freshfarm3.enums.UnitType;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.repository.CategoryRepository;
import com.example.freshfarm3.repository.ShopRepository;
import com.example.freshfarm3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository    productRepository;
    private final ShopRepository     shopRepository;
    private final CategoryRepository   categoryRepository;
    private final FileUploadService    fileUploadService;

    // ── CREATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse createProduct(ProductRequest req,
                                         String shopEmail,
                                         List<MultipartFile> images) {
        Shop shop = shopRepository.findByUser_Email(shopEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        UnitType unitType = resolveUnitType(req.getUnitType());
        validatePieceWeight(unitType, req.getAvgPieceWeightGrams());

        BigDecimal quantity = req.getQuantity() != null ? req.getQuantity() : BigDecimal.ZERO;

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .unitType(unitType)
                .avgPieceWeightGrams(req.getAvgPieceWeightGrams())
                .stockQuantity(quantity)
                .isAvailable(quantity.compareTo(BigDecimal.ZERO) > 0)
                .status(resolveStatus(req.getStatus()))
                .isOrganic(false) // TODO: ProductRequest has no isOrganic field yet
                .category(category)
                .shop(shop)
                .build();

        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                String url = fileUploadService.uploadFile(file);
                ProductImage pi = ProductImage.builder()
                        .imageUrl(url)
                        .product(product)
                        .displayOrder(i)
                        .isPrimary(i == 0) // first uploaded image becomes primary
                        .build();
                product.getImages().add(pi);
            }
        }

        Product saved = productRepository.save(product);
        log.info("Product created: {} by shop: {}", saved.getId(), shopEmail);
        return mapToResponse(saved);
    }

    // ── UPDATE ───────────────────────────────────────────────────
    @Transactional
    public ProductResponse updateProduct(Long productId,
                                         ProductRequest req,
                                         String shopEmail) {
        Product product = getProductOwnedByShop(productId, shopEmail);

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());

        if (req.getUnitType() != null) {
            UnitType unitType = resolveUnitType(req.getUnitType());
            validatePieceWeight(unitType, req.getAvgPieceWeightGrams());
            product.setUnitType(unitType);
            product.setAvgPieceWeightGrams(req.getAvgPieceWeightGrams());
        }

        BigDecimal quantity = req.getQuantity() != null ? req.getQuantity() : BigDecimal.ZERO;
        product.setStockQuantity(quantity);
        product.setIsAvailable(quantity.compareTo(BigDecimal.ZERO) > 0);

        if (req.getStatus() != null) {
            product.setStatus(resolveStatus(req.getStatus()));
        }

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated: {} by shop: {}", productId, shopEmail);
        return mapToResponse(updated);
    }

    // ── DELETE ───────────────────────────────────────────────────
    @Transactional
    public void deleteProduct(Long productId, String shopEmail) {
        Product product = getProductOwnedByShop(productId, shopEmail);
        productRepository.delete(product);
        log.info("Product deleted: {} by shop: {}", productId, shopEmail);
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
    public Page<ProductResponse> filterByCategory(Long categoryId, Pageable pageable) {
        return productRepository
                .findByCategory_IdAndStatus(categoryId, Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(String shopEmail) {
        Shop shop = shopRepository.findByUser_Email(shopEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        return productRepository.findByShop(shop).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Sprint 3: Stock Validation ───────────────────────────────
    // Quantity here is the buyer-facing order amount (kg/L/pieces per the
    // product's unitType), not a raw stock unit — Product.hasStock already
    // converts it internally via stockDeltaFor().
    @Transactional(readOnly = true)
    public void validateStock(Long productId, BigDecimal requestedQty) {
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
    public void deductStock(Long productId, BigDecimal qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.deductStock(qty);
        productRepository.save(product);
        log.info("Stock deducted: productId={} qty={} remaining={}", productId, qty, product.getStockQuantity());
    }

    @Transactional
    public void restoreStock(Long productId, BigDecimal qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.restoreStock(qty);
        productRepository.save(product);
        log.info("Stock restored: productId={} qty={}", productId, qty);
    }

    // ── INTERNAL HELPERS ─────────────────────────────────────────
    private Product getProductOwnedByShop(Long productId, String shopEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!product.getShop().getUser().getEmail().equals(shopEmail)) {
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

    private UnitType resolveUnitType(String unitType) {
        if (unitType == null || unitType.isBlank()) {
            throw new IllegalArgumentException("unitType is required. Must be one of: KG, PIECE, LITER");
        }
        try {
            return UnitType.valueOf(unitType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid unitType '" + unitType + "'. Must be one of: KG, PIECE, LITER");
        }
    }

    // PIECE-sold products (e.g. bananas) need an average piece weight so
    // stock — always tracked in kg — can be deducted accurately when a
    // buyer orders by piece count. KG/LITER products don't need this.
    private void validatePieceWeight(UnitType unitType, BigDecimal avgPieceWeightGrams) {
        if (unitType == UnitType.PIECE &&
                (avgPieceWeightGrams == null || avgPieceWeightGrams.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                    "avgPieceWeightGrams is required and must be greater than 0 for PIECE products");
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
                .unitType(p.getUnitType() != null ? p.getUnitType().name() : null)
                .avgPieceWeightGrams(p.getAvgPieceWeightGrams())
                .approxPiecesAvailable(p.approxPiecesAvailable())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .shopId(p.getShop().getId())
                .ownerName(p.getShop().getUser().getFullName())
                .shopName(p.getShop().getShopName())
                .primaryImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .imageUrls(imageUrls)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}