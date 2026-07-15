package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerOrderByOrderDateDesc(Buyer buyer);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByOrderStatus(OrderStatus status);

    // Shop: find all orders that include their products
    List<Order> findByItems_Product_Shop_User_Email(String shopEmail);

    long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    long countByOrderStatus(OrderStatus orderStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = :paymentStatus")
    BigDecimal sumTotalAmountByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE o.paymentStatus = :paymentStatus
              AND o.orderDate BETWEEN :start AND :end
            """)
    BigDecimal sumTotalAmountByPaymentStatusAndDateBetween(
            @Param("paymentStatus") String paymentStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
