package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // All reviews for a given product
    List<Review> findByProductIdOrderByReviewDateDesc(Long productId);

    // All reviews for a given shop
    List<Review> findByShopIdOrderByReviewDateDesc(Long shopId);

    // All reviews written by a specific buyer
    List<Review> findByBuyerIdOrderByReviewDateDesc(Long buyerId);

    // Check for duplicate: one review per order per buyer
    boolean existsByOrderIdAndBuyerId(Long orderId, Long buyerId);

    // Average rating for a product
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId")
    Double calculateAverageRatingByProduct(@Param("productId") Long productId);

    // Average rating for a shop
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.shop.id = :shopId")
    Double calculateAverageRatingByShop(@Param("shopId") Long shopId);

    // Count of reviews for a product
    long countByProductId(Long productId);

    // Count of reviews for a shop
    long countByShopId(Long shopId);

    // Rating distribution for a product (1–5 star counts)
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionByProduct(@Param("productId") Long productId);

    // Find specific review by order and buyer (for duplicate check with return value)
    Optional<Review> findByOrderIdAndBuyerId(Long orderId, Long buyerId);
}
