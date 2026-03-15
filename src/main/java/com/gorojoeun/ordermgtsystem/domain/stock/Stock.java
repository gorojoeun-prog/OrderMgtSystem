package com.gorojoeun.ordermgtsystem.domain.stock;

import com.gorojoeun.ordermgtsystem.domain.product.Product;
import com.gorojoeun.ordermgtsystem.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    protected Stock() {
    }

    public Stock(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public void decrease(Integer amount) {
        if (quantity < amount) {
            throw new BusinessException("재고가 부족합니다.");
        }
        quantity -= amount;
    }

    public void increase(Integer amount) {
        quantity += amount;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
