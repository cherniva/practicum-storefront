package com.cherniva.storefront.service;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductR2dbcRepository productRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private Integer ttl;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;
    private List<Product> productList;

    @BeforeEach
    void setUp() {
        product1 = new Product();
        product1.setId(1L);
        product1.setName("Test Product 1");
        product1.setPrice(new BigDecimal("100.00"));

        product2 = new Product();
        product2.setId(2L);
        product2.setName("Test Product 2");
        product2.setPrice(new BigDecimal("200.00"));

        productList = Arrays.asList(product1, product2);
    }

    @Test
    void getProductsSortedBy_ShouldReturnSortedProducts() {
        // Arrange
        when(productRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.fromIterable(productList));
        when(productRepository.count()).thenReturn(Mono.just(2L));

        // Act & Assert
        StepVerifier.create(productService.getProductsSortedBy(0, 10, "name", "asc"))
            .assertNext(page -> {
                assertNotNull(page);
                assertEquals(2, page.getContent().size());
                assertEquals("Test Product 1", page.getContent().get(0).getName());
            })
            .verifyComplete();

        verify(productRepository, times(1)).findAllBy(any(Pageable.class));
    }

    @Test
    void getProductsSortedBy_ShouldHandleDescendingSort() {
        // Arrange
        List<Product> sortedList = Arrays.asList(product2, product1); // Higher price first
        when(productRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.fromIterable(sortedList));
        when(productRepository.count()).thenReturn(Mono.just(2L));

        // Act & Assert
        StepVerifier.create(productService.getProductsSortedBy(0, 10, "price", "desc"))
            .assertNext(page -> {
                assertNotNull(page);
                assertEquals(2, page.getContent().size());
                assertEquals(new BigDecimal("200.00"), page.getContent().get(0).getPrice());
            })
            .verifyComplete();

        verify(productRepository, times(1)).findAllBy(any(Pageable.class));
    }

    @Test
    void searchProductsByName_ShouldReturnMatchingProducts() {
        // Arrange
        String keyword = "Test";
        when(productRepository.findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .thenReturn(Flux.fromIterable(productList));
        when(productRepository.count()).thenReturn(Mono.just(2L));

        // Act & Assert
        StepVerifier.create(productService.searchProductsByName(keyword, 0, 10, "name", "asc"))
            .assertNext(page -> {
                assertNotNull(page);
                assertEquals(2, page.getContent().size());
                assertTrue(page.getContent().stream().allMatch(p -> p.getName().contains(keyword)));
            })
            .verifyComplete();

        verify(productRepository, times(1))
                .findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
    }

    @Test
    void searchProductsByName_ShouldHandleEmptyResults() {
        // Arrange
        String keyword = "Nonexistent";
        when(productRepository.findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .thenReturn(Flux.empty());
        when(productRepository.count()).thenReturn(Mono.just(0L));

        // Act & Assert
        StepVerifier.create(productService.searchProductsByName(keyword, 0, 10, "name", "asc"))
            .assertNext(page -> {
                assertNotNull(page);
                assertTrue(page.getContent().isEmpty());
            })
            .verifyComplete();

        verify(productRepository, times(1))
                .findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
    }
} 