package com.gorojoeun.ordermgtsystem.controller;

import com.gorojoeun.ordermgtsystem.IntegrationTestSupport;
import com.gorojoeun.ordermgtsystem.dto.product.CreateProductRequest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends IntegrationTestSupport {

    @Test
    void createProductAndReadProductAndStock() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Mocha", new BigDecimal("5200.00"), 30);

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mocha"))
                .andExpect(jsonPath("$.price").value(5200.00))
                .andExpect(jsonPath("$.stockQuantity").value(30))
                .andReturn();

        Number productIdValue = JsonPath.read(createResult.getResponse().getContentAsString(), "$.productId");
        Long productId = productIdValue.longValue();

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.name").value("Mocha"))
                .andExpect(jsonPath("$.stockQuantity").value(30));

        mockMvc.perform(get("/api/stocks/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("Mocha"))
                .andExpect(jsonPath("$.quantity").value(30));
    }
}
