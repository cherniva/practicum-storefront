package com.cherniva.storefront.repository;

import com.cherniva.storefront.config.TestConfig;
import com.cherniva.storefront.model.CustomerOrder;
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
public class CustomerOrderRepositoryTest {

    @Autowired
    private CustomerOrderR2dbcRepository customerOrderRepository;

    @Test
    void testFindAll() {
        Flux<CustomerOrder> ordersFlux = customerOrderRepository.findAll();
        
        StepVerifier.create(ordersFlux)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void testFindById() {
        Mono<CustomerOrder> orderMono = customerOrderRepository.findById(1L);
        
        StepVerifier.create(orderMono)
            .assertNext(order -> {
                assertThat(order).isNotNull();
            })
            .verifyComplete();
    }
} 