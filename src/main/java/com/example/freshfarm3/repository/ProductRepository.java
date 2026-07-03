package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Category;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── Farmer's own products ─────────────────────────────────
    List<Product> findByFarmer(Farmer farmer);

    List<Product> findByFarmerAndStatus(Farmer farmer, Product.ProductStatus status);

    long countByFarmer(Farmer farmer);

    long countByFarmerAndStatus(Farmer farmer, Product.ProductStatus status);

    // ── Browse by category ────────────────────────────────────
    List<Product> findByCategoryAndStatus(Category category, Product.ProductStatus status);

    List<Product> findByStatus(Product.ProductStatus status);

    // ── Search by name ────────────────────────────────────────
    List<Product> findByNameContainingIgnoreCaseAndStatus(
            String name, Product.ProductStatus status);

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
            @Param("status")  Product.ProductStatus status);

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
            @Param("keyword")  String keyword,
            @Param("status")   Product.ProductStatus status);



    long countByAvailableTrue();

    double findAverageProductRating();

    <T> Optional<T> findByCategory_IdAndStatus(Long categoryId, Product.ProductStatus productStatus, Pageable pageable);
}