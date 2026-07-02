package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByBuyer(Buyer buyer);
}