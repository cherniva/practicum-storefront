package com.cherniva.storefront.repository;

import com.cherniva.storefront.config.TestConfig;
import com.cherniva.storefront.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
public class ProductRepositoryTest {

//    @Autowired
//    private ProductR2dbcRepository productRepository;
//
//    @Test
//    void testGetByName() {
//        List<Product> products = productRepository.getByName("Test Product 1");
//        assertThat(products).hasSize(1);
//        assertThat(products.get(0).getName()).isEqualTo("Test Product 1");
//    }
//
//    @Test
//    void testGetProductsByCountGreaterThanZero() {
//        List<Product> products = productRepository.getProductsByCountGreaterThanZero();
//        assertThat(products).hasSize(2);
//        assertThat(products).allMatch(p -> p.getCount() > 0);
//    }
//
//    @Test
//    void testFindByNameContainingIgnoreCase() {
//        Page<Product> products = productRepository.findByNameContainingIgnoreCase("test", PageRequest.of(0, 10));
//        assertThat(products.getContent()).hasSize(3);
//    }
} 