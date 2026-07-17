package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.ShopPayout;
import com.example.freshfarm3.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ShopPayoutRepository extends JpaRepository<ShopPayout, Long> {

    List<ShopPayout> findByShopOrderByCreatedAtDesc(Shop shop);

    List<ShopPayout> findAllByOrderByCreatedAtDesc();

    List<ShopPayout> findByShopAndStatus(Shop shop, PayoutStatus status);

    default BigDecimal totalForShopByStatus(Shop shop, PayoutStatus status) {
        return findByShopAndStatus(shop, status).stream()
                .map(ShopPayout::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
