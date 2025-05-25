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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

//    @Mock
//    private ProductR2dbcRepository productRepository;
//
//    @InjectMocks
//    private ProductService productService;
//
//    private Product product1;
//    private Product product2;
//    private List<Product> productList;
//    private Page<Product> productPage;
//
//    @BeforeEach
//    void setUp() {
//        product1 = new Product();
//        product1.setId(1L);
//        product1.setName("Test Product 1");
//        product1.setPrice(new BigDecimal("100.00"));
//
//        product2 = new Product();
//        product2.setId(2L);
//        product2.setName("Test Product 2");
//        product2.setPrice(new BigDecimal("200.00"));
//
//        productList = Arrays.asList(product1, product2);
//        productPage = new PageImpl<>(productList);
//    }
//
//    @Test
//    void getProductsSortedBy_ShouldReturnSortedProducts() {
//        // Arrange
//        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
//
//        // Act
//        Page<Product> result = productService.getProductsSortedBy(0, 10, "name", "asc");
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getContent().size());
//        verify(productRepository, times(1)).findAll(any(Pageable.class));
//    }
//
//    @Test
//    void getProductsSortedBy_ShouldHandleDescendingSort() {
//        // Arrange
//        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
//
//        // Act
//        Page<Product> result = productService.getProductsSortedBy(0, 10, "price", "desc");
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getContent().size());
//        verify(productRepository, times(1)).findAll(any(Pageable.class));
//    }
//
//    @Test
//    void searchProductsByName_ShouldReturnMatchingProducts() {
//        // Arrange
//        String keyword = "Test";
//        when(productRepository.findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
//                .thenReturn(productPage);
//
//        // Act
//        Page<Product> result = productService.searchProductsByName(keyword, 0, 10, "name", "asc");
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getContent().size());
//        verify(productRepository, times(1))
//                .findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
//    }
//
//    @Test
//    void searchProductsByName_ShouldHandleEmptyResults() {
//        // Arrange
//        String keyword = "Nonexistent";
//        Page<Product> emptyPage = new PageImpl<>(List.of());
//        when(productRepository.findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
//                .thenReturn(emptyPage);
//
//        // Act
//        Page<Product> result = productService.searchProductsByName(keyword, 0, 10, "name", "asc");
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.getContent().isEmpty());
//        verify(productRepository, times(1))
//                .findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
//    }
} 