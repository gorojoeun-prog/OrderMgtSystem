package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.dto.stock.StockResponse;

public interface StockService {

    StockResponse getStockByProductId(Long productId);
}
