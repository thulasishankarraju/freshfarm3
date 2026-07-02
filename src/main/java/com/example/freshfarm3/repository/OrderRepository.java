package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerOrderByOrderDateDesc(Buyer buyer);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByOrderStatus(OrderStatus status);

    // Farmer: find all orders that include their products
    List<Order> findByItems_Product_Farmer_User_Email(String farmerEmail);
}