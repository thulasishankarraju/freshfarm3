package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductOrderByDisplayOrderAsc(Product product);

    void deleteByProduct(Product product);
}
