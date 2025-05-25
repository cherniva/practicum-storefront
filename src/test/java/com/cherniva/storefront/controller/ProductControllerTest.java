package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductR2dbcRepository productRepository;

    @Test
    public void testGetProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));

        when(productRepository.findById(1L)).thenReturn(Mono.just(product));

        webTestClient.get()
                .uri("/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Test Product"), "Response should contain product name");
                    assertTrue(body.contains("Test Description"), "Response should contain product description");
                    assertTrue(body.contains("99.99"), "Response should contain product price");
                });
    }

    @Test
    public void testAddToCartPlus() {
        Product product = new Product();
        product.setId(1L);
        product.setCount(0);

        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        webTestClient.post()
                .uri("/products/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=plus")
                .header("referer", "/products/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/products/1");
    }

    @Test
    public void testGetNewProductForm() {
        webTestClient.get()
                .uri("/products/new")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Добавить новый товар"), "Response should contain form title");
                    assertTrue(body.contains("name"), "Response should contain name field");
                    assertTrue(body.contains("description"), "Response should contain description field");
                    assertTrue(body.contains("price"), "Response should contain price field");
                });
    }

    @Test
    public void testAddNewProduct() {
        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("New Product");
        savedProduct.setDescription("New Description");
        savedProduct.setPrice(new BigDecimal("149.99"));

        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(savedProduct));

        webTestClient.post()
                .uri("/products/new")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue("name=New Product&description=New Description&price=149.99")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/products");
    }
} 