package com.gorojoeun.ordermgtsystem.repository;

import com.gorojoeun.ordermgtsystem.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
