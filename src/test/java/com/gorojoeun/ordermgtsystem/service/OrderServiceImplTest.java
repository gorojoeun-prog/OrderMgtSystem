package com.gorojoeun.ordermgtsystem.service;

import com.gorojoeun.ordermgtsystem.domain.order.Order;
import com.gorojoeun.ordermgtsystem.domain.product.Product;
import com.gorojoeun.ordermgtsystem.domain.stock.Stock;
import com.gorojoeun.ordermgtsystem.dto.order.CreateOrderRequest;
import com.gorojoeun.ordermgtsystem.dto.order.OrderResponse;
import com.gorojoeun.ordermgtsystem.exception.BusinessException;
import com.gorojoeun.ordermgtsystem.exception.NotFoundException;
import com.gorojoeun.ordermgtsystem.repository.OrderRepository;
import com.gorojoeun.ordermgtsystem.repository.ProductRepository;
import com.gorojoeun.ordermgtsystem.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * OrderServiceImpl.createOrder() 경계값 분석(BVA) 단위 테스트
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │             경계값 분석(BVA) 시나리오 정의                              │
 * ├──────┬────────┬────────┬────────────────────────┬────────────────────┤
 * │ TC#  │ 재고   │ 주문량 │ 기대 결과              │ 경계값 유형        │
 * ├──────┼────────┼────────┼────────────────────────┼────────────────────┤
 * │  BV1 │   0    │   1    │ BusinessException (실패)│ 하한 경계값 - 1    │
 * │  BV2 │   1    │   1    │ 주문 성공 (재고 0 소진) │ 하한 경계값 (min)  │
 * │  BV3 │   2    │   1    │ 주문 성공 (재고 1 남음) │ 하한 경계값 + 1    │
 * ├──────┼────────┼────────┼────────────────────────┼────────────────────┤
 * │  BV4 │  10    │   9    │ 주문 성공 (재고 1 남음) │ 상한 경계값 - 1    │
 * │  BV5 │  10    │  10    │ 주문 성공 (재고 0 소진) │ 상한 경계값 (max)  │
 * │  BV6 │  10    │  11    │ BusinessException (실패)│ 상한 경계값 + 1    │
 * ├──────┼────────┼────────┼────────────────────────┼────────────────────┤
 * │  BV7 │   -    │   1    │ NotFoundException       │ 비정상: 상품 없음  │
 * │  BV8 │   -    │   1    │ NotFoundException       │ 비정상: 재고 없음  │
 * └──────┴────────┴────────┴────────────────────────┴────────────────────┘
 *
 * 핵심 경계 조건: Stock.decrease() → quantity < amount 이면 BusinessException
 *   - quantity = amount     : 정확히 소진 → 성공 (등호 포함)
 *   - quantity = amount - 1 : 1개 부족    → 실패
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl.createOrder() - 경계값 분석(BVA) 단위 테스트")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ── 공통 픽스처 ─────────────────────────────────────────────────────────

    private static final Long   PRODUCT_ID   = 1L;
    private static final String PRODUCT_NAME = "Americano";
    private static final BigDecimal UNIT_PRICE = new BigDecimal("3500.00");

    private Product createProduct() {
        Product product = new Product(PRODUCT_NAME, UNIT_PRICE);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }

    /** 저장 후 반환될 Order Mock (id 주입) */
    private Order createSavedOrder(Product product, int quantity, long orderId) {
        Order order = Order.create(product, quantity, UNIT_PRICE.multiply(BigDecimal.valueOf(quantity)));
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    // ════════════════════════════════════════════════════════════════════════
    // BV1 ~ BV3 : 재고 경계값 분석 (주문량 = 1 고정)
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("재고 경계값 (주문량=1 고정)")
    class StockBoundaryTests {

        /**
         * BV1 - 하한 경계값 - 1
         * 재고=0, 주문량=1 → quantity(0) < amount(1) → BusinessException
         */
        @Test
        @DisplayName("BV1: 재고=0, 주문량=1 → 재고 부족 예외 (하한 경계값 미만)")
        void bv1_stockZero_quantityOne_throwsBusinessException() {
            // given
            Product product = createProduct();
            Stock stock = new Stock(product, 0);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

            // when & then
            assertThatThrownBy(() ->
                    orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("재고가 부족합니다.");

            verify(orderRepository, never()).save(any());
        }

        /**
         * BV2 - 하한 경계값 (최솟값)
         * 재고=1, 주문량=1 → quantity(1) >= amount(1) → 성공, 재고 0으로 소진
         */
        @Test
        @DisplayName("BV2: 재고=1, 주문량=1 → 주문 성공, 재고 0으로 소진 (하한 경계값)")
        void bv2_stockOne_quantityOne_succeedsAndDepletsStock() {
            // given
            Product product = createProduct();
            Stock stock = new Stock(product, 1);
            Order savedOrder = createSavedOrder(product, 1, 10L);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));
            given(orderRepository.save(any())).willReturn(savedOrder);

            // when
            OrderResponse response = orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 1));

            // then - 응답 검증
            assertThat(response.orderId()).isEqualTo(10L);
            assertThat(response.productId()).isEqualTo(PRODUCT_ID);
            assertThat(response.productName()).isEqualTo(PRODUCT_NAME);
            assertThat(response.quantity()).isEqualTo(1);
            assertThat(response.totalPrice()).isEqualByComparingTo(UNIT_PRICE);
            assertThat(response.status()).isEqualTo("CREATED");

            // then - 재고 차감 검증 (0으로 소진)
            assertThat(stock.getQuantity()).isZero();
        }

        /**
         * BV3 - 하한 경계값 + 1
         * 재고=2, 주문량=1 → quantity(2) >= amount(1) → 성공, 재고 1 남음
         */
        @Test
        @DisplayName("BV3: 재고=2, 주문량=1 → 주문 성공, 재고 1 남음 (하한 경계값+1)")
        void bv3_stockTwo_quantityOne_succeedsAndLeavesOne() {
            // given
            Product product = createProduct();
            Stock stock = new Stock(product, 2);
            Order savedOrder = createSavedOrder(product, 1, 11L);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));
            given(orderRepository.save(any())).willReturn(savedOrder);

            // when
            orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 1));

            // then - 재고 1 남음
            assertThat(stock.getQuantity()).isEqualTo(1);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // BV4 ~ BV6 : 주문량 경계값 분석 (재고 = 10 고정)
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("주문량 경계값 (재고=10 고정)")
    class OrderQuantityBoundaryTests {

        /**
         * BV4 - 상한 경계값 - 1
         * 재고=10, 주문량=9 → quantity(10) >= amount(9) → 성공, 재고 1 남음
         */
        @Test
        @DisplayName("BV4: 재고=10, 주문량=9 → 주문 성공, 재고 1 남음 (상한 경계값-1)")
        void bv4_stock10_quantity9_succeedsAndLeavesOne() {
            // given
            Product product = createProduct();
            Stock stock = new Stock(product, 10);
            Order savedOrder = createSavedOrder(product, 9, 20L);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));
            given(orderRepository.save(any())).willReturn(savedOrder);

            // when
            OrderResponse response = orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 9));

            // then
            assertThat(response.quantity()).isEqualTo(9);
            assertThat(stock.getQuantity()).isEqualTo(1);
        }

        /**
         * BV5 - 상한 경계값 (정확히 소진)
         * 재고=10, 주문량=10 → quantity(10) >= amount(10) → 성공, 재고 0으로 소진
         */
        @Test
        @DisplayName("BV5: 재고=10, 주문량=10 → 주문 성공, 재고 0으로 소진 (상한 경계값)")
        void bv5_stock10_quantity10_succeedsAndDepletsStock() {
            // given
            Product product = createProduct();
            Stock stock = new Stock(product, 10);
            Order savedOrder = createSavedOrder(product, 10, 21L);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));
            given(orderRepository.save(any())).willReturn(savedOrder);

            // when
            OrderResponse response = orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 10));

            // then
            assertThat(response.quantity()).isEqualTo(10);
            assertThat(stock.getQuantity()).isZero();
        }

        /**
         * BV6 - 상한 경계값 + 1
         * 재고=10, 주문량=11 → quantity(10) < amount(11) → BusinessException
         */
        @Test
        @DisplayName("BV6: 재고=10, 주문량=11 → 재고 부족 예외 (상한 경계값+1)")
        void bv6_stock10_quantity11_throwsBusinessException() {
            // given
            Product product = createProduct();
            Stock stock = new Stock(product, 10);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

            // when & then
            assertThatThrownBy(() ->
                    orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 11)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("재고가 부족합니다.");

            verify(orderRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // BV7 ~ BV8 : 비정상 입력 (존재하지 않는 엔티티)
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("비정상 입력 (엔티티 미존재)")
    class InvalidInputTests {

        /**
         * BV7 - 상품 미존재
         * ProductRepository가 empty를 반환하면 NotFoundException
         */
        @Test
        @DisplayName("BV7: 상품을 찾을 수 없으면 NotFoundException 발생")
        void bv7_productNotFound_throwsNotFoundException() {
            // given
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 1)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다.");

            verify(stockRepository, never()).findByProductIdForUpdate(any());
            verify(orderRepository, never()).save(any());
        }

        /**
         * BV8 - 재고 레코드 미존재
         * 상품은 있지만 재고 row가 없으면 NotFoundException
         */
        @Test
        @DisplayName("BV8: 재고 정보를 찾을 수 없으면 NotFoundException 발생")
        void bv8_stockNotFound_throwsNotFoundException() {
            // given
            Product product = createProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    orderService.createOrder(new CreateOrderRequest(PRODUCT_ID, 1)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("재고 정보를 찾을 수 없습니다.");

            verify(orderRepository, never()).save(any());
        }
    }
}
