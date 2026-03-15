package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.domain.order.Order;
import com.gorojoeun.ordermgtsystem.domain.product.Product;
import com.gorojoeun.ordermgtsystem.domain.stock.Stock;
import com.gorojoeun.ordermgtsystem.dto.order.CreateOrderRequest;
import com.gorojoeun.ordermgtsystem.dto.order.OrderResponse;
import com.gorojoeun.ordermgtsystem.exception.NotFoundException;
import com.gorojoeun.ordermgtsystem.repository.OrderRepository;
import com.gorojoeun.ordermgtsystem.repository.ProductRepository;
import com.gorojoeun.ordermgtsystem.repository.StockRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            StockRepository stockRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다. id=" + request.productId()));

        Stock stock = stockRepository.findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new NotFoundException("재고 정보를 찾을 수 없습니다. productId=" + request.productId()));

        stock.decrease(request.quantity());

        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        Order order = Order.create(product, request.quantity(), totalPrice);
        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다. id=" + orderId));

        Stock stock = stockRepository.findByProductIdForUpdate(order.getProduct().getId())
                .orElseThrow(() -> new NotFoundException("재고 정보를 찾을 수 없습니다. productId=" + order.getProduct().getId()));

        order.cancel();
        stock.increase(order.getQuantity());

        return OrderResponse.from(order);
    }
}
