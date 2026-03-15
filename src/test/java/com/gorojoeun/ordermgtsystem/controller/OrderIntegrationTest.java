package com.gorojoeun.ordermgtsystem.controller;

import com.gorojoeun.ordermgtsystem.IntegrationTestSupport;
import com.gorojoeun.ordermgtsystem.dto.order.CreateOrderRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIntegrationTest extends IntegrationTestSupport {

    @Test
    void createAndCancelOrderShouldRestoreStock() throws Exception {
        MvcResult stockBeforeResult = mockMvc.perform(get("/api/stocks/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andReturn();

        Integer quantityBefore = JsonPath.read(stockBeforeResult.getResponse().getContentAsString(), "$.quantity");

        CreateOrderRequest createRequest = new CreateOrderRequest(1L, 2);
        MvcResult createOrderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        Number orderIdValue = JsonPath.read(createOrderResult.getResponse().getContentAsString(), "$.orderId");
        Long orderId = orderIdValue.longValue();

        mockMvc.perform(get("/api/stocks/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(quantityBefore - 2));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/stocks/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(quantityBefore));
    }

    @Test
    void createOrderShouldFailWhenStockInsufficient() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1_000_000);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("재고가 부족합니다.")));
    }
}
