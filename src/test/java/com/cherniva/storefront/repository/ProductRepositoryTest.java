package com.cherniva.storefront.repository;

import com.cherniva.storefront.config.TestConfig;
import com.cherniva.storefront.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(TestConfig.class)
public class ProductRepositoryTest {

    @Autowired
    private ProductR2dbcRepository productRepository;

    @Test
    void testGetByName() {
        Flux<Product> productsFlux = productRepository.getByName("Test Product 1");
        
        StepVerifier.create(productsFlux.collectList())
            .assertNext(products -> {
                assertThat(products).hasSize(1);
                assertThat(products.get(0).getName()).isEqualTo("Test Product 1");
            })
            .verifyComplete();
    }

    @Test
    void testGetProductsByCountGreaterThanZero() {
        Flux<Product> productsFlux = productRepository.getProductsByCountGreaterThanZero();
        
        StepVerifier.create(productsFlux.collectList())
            .assertNext(products -> {
                assertThat(products).hasSize(2);
                assertThat(products).allMatch(p -> p.getCount() > 0);
            })
            .verifyComplete();
    }

    @Test
    void testFindByNameContainingIgnoreCase() {
        Flux<Product> productsFlux = productRepository.findByNameContainingIgnoreCase("test", PageRequest.of(0, 10));
        
        StepVerifier.create(productsFlux.collectList())
            .assertNext(products -> {
                assertThat(products).hasSize(3);
            })
            .verifyComplete();
    }
} 