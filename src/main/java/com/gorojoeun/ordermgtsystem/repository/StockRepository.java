package com.gorojoeun.ordermgtsystem.repository;

import com.gorojoeun.ordermgtsystem.domain.stock.Stock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("select s from Stock s join fetch s.product p where p.id = :productId")
    Optional<Stock> findByProductId(@Param("productId") Long productId);

    @Query("select s from Stock s join fetch s.product p")
    java.util.List<Stock> findAllWithProduct();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s join fetch s.product p where p.id = :productId")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") Long productId);
}
