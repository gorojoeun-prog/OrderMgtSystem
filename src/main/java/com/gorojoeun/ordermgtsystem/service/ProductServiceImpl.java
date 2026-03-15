package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.domain.product.Product;
import com.gorojoeun.ordermgtsystem.domain.stock.Stock;
import com.gorojoeun.ordermgtsystem.dto.product.CreateProductRequest;
import com.gorojoeun.ordermgtsystem.dto.product.ProductResponse;
import com.gorojoeun.ordermgtsystem.exception.NotFoundException;
import com.gorojoeun.ordermgtsystem.repository.ProductRepository;
import com.gorojoeun.ordermgtsystem.repository.StockRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public ProductServiceImpl(ProductRepository productRepository, StockRepository stockRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product savedProduct = productRepository.save(new Product(request.name(), request.price()));
        Stock savedStock = stockRepository.save(new Stock(savedProduct, request.initialStockQuantity()));
        return toProductResponse(savedProduct, savedStock);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다. id=" + productId));
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("재고 정보를 찾을 수 없습니다. productId=" + productId));
        return toProductResponse(product, stock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        return stockRepository.findAllWithProduct().stream()
                .map(stock -> toProductResponse(stock.getProduct(), stock))
                .toList();
    }

    private ProductResponse toProductResponse(Product product, Stock stock) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                stock.getQuantity()
        );
    }
}
