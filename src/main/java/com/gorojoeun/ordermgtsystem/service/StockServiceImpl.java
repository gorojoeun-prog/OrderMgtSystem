package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.domain.stock.Stock;
import com.gorojoeun.ordermgtsystem.dto.stock.StockResponse;
import com.gorojoeun.ordermgtsystem.exception.NotFoundException;
import com.gorojoeun.ordermgtsystem.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;

    public StockServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponse getStockByProductId(Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("재고 정보를 찾을 수 없습니다. productId=" + productId));

        return new StockResponse(
                stock.getProduct().getId(),
                stock.getProduct().getName(),
                stock.getQuantity()
        );
    }
}
