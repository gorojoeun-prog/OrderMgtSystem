package com.gorojoeun.ordermgtsystem.dto.stock;

public record StockResponse(
        Long productId,
        String productName,
        Integer quantity
) {
}
