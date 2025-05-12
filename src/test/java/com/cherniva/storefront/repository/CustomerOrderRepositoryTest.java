package com.cherniva.storefront.repository;

import com.cherniva.storefront.config.TestConfig;
import com.cherniva.storefront.model.CustomerOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
public class CustomerOrderRepositoryTest {

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Test
    void testFindAll() {
        List<CustomerOrder> orders = customerOrderRepository.findAll();
        assertThat(orders).hasSize(2);
    }

    @Test
    void testFindById() {
        CustomerOrder order = customerOrderRepository.findById(1L).orElse(null);
        assertThat(order).isNotNull();
        assertThat(!order.getProducts().isEmpty());
    }
} 