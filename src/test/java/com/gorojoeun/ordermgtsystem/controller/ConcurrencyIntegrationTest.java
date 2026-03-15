package com.gorojoeun.ordermgtsystem.controller;

import com.gorojoeun.ordermgtsystem.IntegrationTestSupport;
import com.gorojoeun.ordermgtsystem.dto.order.CreateOrderRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConcurrencyIntegrationTest extends IntegrationTestSupport {

    /**
     * 10개 스레드가 동시에 주문할 때, 비관적 락(PESSIMISTIC_WRITE)이 초과 판매를 방지하는지 검증한다.
     * - 상품 id=2 (Latte) 초기 재고: 80
     * - 주문당 수량: 20 → 최대 4건만 성공 가능
     * - 나머지 6건은 재고 부족(400)으로 실패해야 함
     */
    @Test
    void concurrentOrdersShouldNotOversellStock() throws Exception {
        // Given: 현재 재고 확인
        MvcResult stockBeforeResult = mockMvc.perform(get("/api/stocks/products/{productId}", 2L))
                .andExpect(status().isOk())
                .andReturn();
        Integer initialStock = JsonPath.read(stockBeforeResult.getResponse().getContentAsString(), "$.quantity");

        int threadCount = 10;
        int quantityPerOrder = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);   // 전 스레드 동시 출발
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        String requestBody = objectMapper.writeValueAsString(new CreateOrderRequest(2L, quantityPerOrder));

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn();
                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // When: 모든 스레드 동시 출발
        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(finished).as("모든 스레드가 30초 내에 완료되어야 합니다").isTrue();

        // 성공 건수 = 초기 재고 / 주문당 수량 (정확히 딱 떨어지는 경우)
        int expectedSuccessCount = initialStock / quantityPerOrder; // 80 / 20 = 4
        assertThat(successCount.get())
                .as("재고 범위 내에서만 주문이 성공해야 합니다")
                .isEqualTo(expectedSuccessCount);
        assertThat(failCount.get())
                .as("재고 초과 주문은 실패해야 합니다")
                .isEqualTo(threadCount - expectedSuccessCount);

        // 최종 재고 = 0 (초과 판매 없음)
        MvcResult stockAfterResult = mockMvc.perform(get("/api/stocks/products/{productId}", 2L))
                .andExpect(status().isOk())
                .andReturn();
        Integer finalStock = JsonPath.read(stockAfterResult.getResponse().getContentAsString(), "$.quantity");

        assertThat(finalStock)
                .as("최종 재고는 음수가 되어서는 안 됩니다 (초과 판매 방지)")
                .isGreaterThanOrEqualTo(0);
        assertThat(finalStock)
                .as("성공한 주문만큼 재고가 차감되어야 합니다")
                .isEqualTo(initialStock - (successCount.get() * quantityPerOrder));
    }
}
