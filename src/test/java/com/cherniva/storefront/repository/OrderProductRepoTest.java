package com.cherniva.storefront.repository;

import com.cherniva.storefront.config.TestConfig;
import com.cherniva.storefront.model.OrderProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
public class OrderProductRepoTest {

    @Autowired
    private OrderProductRepo orderProductRepo;

    @Test
    void testFindAll() {
        List<OrderProduct> orderProducts = orderProductRepo.findAll();
        assertThat(orderProducts).hasSize(2);
    }

    @Test
    void testFindById() {
        OrderProduct orderProduct = orderProductRepo.findById(1L).orElse(null);
        assertThat(orderProduct).isNotNull();
        assertThat(orderProduct.getQuantity()).isEqualTo(2);
    }
} 