package com.gorojoeun.ordermgtsystem.dto.order;

import com.gorojoeun.ordermgtsystem.domain.order.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal totalPrice,
        String status,
        LocalDateTime orderedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProduct().getId(),
                order.getProduct().getName(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getOrderedAt()
        );
    }
}
