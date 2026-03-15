package com.gorojoeun.ordermgtsystem.dto.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String name,
        BigDecimal price,
        Integer stockQuantity
) {
}
