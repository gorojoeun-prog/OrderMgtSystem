package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.dto.product.CreateProductRequest;
import com.gorojoeun.ordermgtsystem.dto.product.ProductResponse;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProduct(Long productId);

    List<ProductResponse> getProducts();
}
