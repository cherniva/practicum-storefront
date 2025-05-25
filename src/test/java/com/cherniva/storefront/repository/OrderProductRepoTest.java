package com.cherniva.storefront.repository;

import com.cherniva.storefront.config.TestConfig;
import com.cherniva.storefront.model.OrderProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(TestConfig.class)
public class OrderProductRepoTest {

    @Autowired
    private OrderProductR2dbcRepository orderProductRepo;

    @Test
    void testFindAll() {
        Flux<OrderProduct> orderProductsFlux = orderProductRepo.findAll();
        
        StepVerifier.create(orderProductsFlux)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void testFindById() {
        Mono<OrderProduct> orderProductMono = orderProductRepo.findById(1L);
        
        StepVerifier.create(orderProductMono)
            .assertNext(orderProduct -> {
                assertThat(orderProduct).isNotNull();
                assertThat(orderProduct.getQuantity()).isEqualTo(2);
            })
            .verifyComplete();
    }
} 