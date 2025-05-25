package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
public class CartControllerTest {

//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private ProductR2dbcRepository productRepository;
//
//    @Test
//    public void testGetCart() throws Exception {
//        // Create test products
//        Product product1 = new Product();
//        product1.setId(1L);
//        product1.setName("Product 1");
//        product1.setCount(2);
//        product1.setPrice(new BigDecimal("10.00"));
//
//        Product product2 = new Product();
//        product2.setId(2L);
//        product2.setName("Product 2");
//        product2.setCount(1);
//        product2.setPrice(new BigDecimal("20.00"));
//
//        List<Product> productsInCart = Arrays.asList(product1, product2);
//
//        when(productRepository.getProductsByCountGreaterThanZero()).thenReturn(productsInCart);
//
//        mockMvc.perform(get("/cart/products"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("cart"))
//                .andExpect(model().attribute("products", productsInCart))
//                .andExpect(model().attribute("empty", false))
//                .andExpect(model().attributeExists("total"));
//    }
//
//    @Test
//    public void testGetEmptyCart() throws Exception {
//        when(productRepository.getProductsByCountGreaterThanZero()).thenReturn(List.of());
//
//        mockMvc.perform(get("/cart/products"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("cart"))
//                .andExpect(model().attribute("products", List.of()))
//                .andExpect(model().attribute("empty", true))
//                .andExpect(model().attributeExists("total"));
//    }
} 