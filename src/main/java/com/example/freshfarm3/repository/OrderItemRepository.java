package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByProduct_Id(Long productId);
}
