package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(MainController.class)
public class MainControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    @Test
    public void testGetProductsDefault() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        webTestClient.get()
                .uri("/main/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                    assertTrue(body.contains("Product 2"), "Response should contain Product 2");
                    assertTrue(body.contains("Product 3"), "Response should contain Product 3");
                    assertTrue(body.contains("Страница: 1"), "Response should show page number");
                });
    }

    @Test
    public void testGetProductsWithSearch() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        when(productService.searchProductsByName(anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/products")
                        .queryParam("search", "test")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("test"), "Response should contain search term");
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                });
    }

    @Test
    public void testGetProductsWithSort() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/products")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("PRICE"), "Response should contain sort parameter");
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                });
    }

    private List<Product> createTestProducts() {
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("10.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("20.00"));

        Product product3 = new Product();
        product3.setId(3L);
        product3.setName("Product 3");
        product3.setPrice(new BigDecimal("30.00"));

        return Arrays.asList(product1, product2, product3);
    }
} 