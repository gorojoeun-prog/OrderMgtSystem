package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.dto.order.CreateOrderRequest;
import com.gorojoeun.ordermgtsystem.dto.order.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrder(Long orderId);

    OrderResponse cancelOrder(Long orderId);
}
