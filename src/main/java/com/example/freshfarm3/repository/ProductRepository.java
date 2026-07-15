package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── Shop's own products ─────────────────────────────────
    List<Product> findByShop(Shop shop);

    List<Product> findByShopAndStatus(Shop shop, Product.ProductStatus status);

    long countByShop(Shop shop);

    long countByShopAndStatus(Shop shop, Product.ProductStatus status);

    // ── Browse by category ────────────────────────────────────
    List<Product> findByCategoryAndStatus(Category category, Product.ProductStatus status);

    List<Product> findByStatus(Product.ProductStatus status);

    // ── Paginated variants used by ProductService ──────────────
    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);

    Page<Product> findByCategory_IdAndStatus(
            Long categoryId, Product.ProductStatus status, Pageable pageable);

    // ── Search by name ────────────────────────────────────────
    List<Product> findByNameContainingIgnoreCaseAndStatus(
            String name, Product.ProductStatus status);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(
            String name, Product.ProductStatus status, Pageable pageable);

    // ── Keyword search across name + description ──────────────
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (LOWER(p.name)        LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR  LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    List<Product> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("status") Product.ProductStatus status);

    // ── Filter by category + optional keyword ─────────────────
    @Query("""
            SELECT p FROM Product p
            WHERE p.status   = :status
              AND p.category = :category
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR  LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    List<Product> searchByCategoryAndKeyword(
            @Param("category") Category category,
            @Param("keyword") String keyword,
            @Param("status") Product.ProductStatus status);

    long countByIsAvailableTrue();

    @Query("SELECT COALESCE(AVG(p.averageRating), 0) FROM Product p")
    Double findAverageProductRating();
}