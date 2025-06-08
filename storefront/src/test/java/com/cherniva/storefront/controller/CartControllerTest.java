package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@WebFluxTest(CartController.class)
public class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductR2dbcRepository productRepository;
    @MockBean
    private PaymentService paymentService;

    @Test
    public void testGetCart() {
        // Create test products
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setCount(2);
        product1.setPrice(new BigDecimal("10.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setCount(1);
        product2.setPrice(new BigDecimal("20.00"));

        List<Product> productsInCart = Arrays.asList(product1, product2);

        when(productRepository.getProductsByCountGreaterThanZero())
            .thenReturn(Flux.fromIterable(productsInCart));
        when(paymentService.getBalance()).thenReturn(Mono.just(1000.0));

        webTestClient.get()
                .uri("/cart/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                    assertTrue(body.contains("Product 2"), "Response should contain Product 2");
                    assertTrue(body.contains("40.00"), "Response should contain total amount of 40.00");
                    assertTrue(body.contains("<button>Купить</button>"), "Response should contain Buy button");
                });
    }

    @Test
    public void testGetEmptyCart() {
        when(productRepository.getProductsByCountGreaterThanZero())
            .thenReturn(Flux.empty());
        when(paymentService.getBalance()).thenReturn(Mono.just(1000.0));

        webTestClient.get()
                .uri("/cart/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertFalse(body.contains("<button>Купить</button>"), "Response should not contain Buy button for empty cart");
                });
    }
} 