package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MainController.class)
public class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    public void testGetProductsDefault() throws Exception {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(productPage);

        mockMvc.perform(get("/main/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("paging"));
    }

    @Test
    public void testGetProductsWithSearch() throws Exception {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        when(productService.searchProductsByName(anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(productPage);

        mockMvc.perform(get("/main/products")
                .param("search", "test")
                .param("pageNumber", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attribute("search", "test"));
    }

    @Test
    public void testGetProductsWithSort() throws Exception {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(productPage);

        mockMvc.perform(get("/main/products")
                .param("sort", "PRICE")
                .param("pageNumber", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attribute("sort", "PRICE"));
    }

    private List<Product> createTestProducts() {
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("10.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("20.00"));

        Product product3 = new Product();
        product3.setId(3L);
        product3.setName("Product 3");
        product3.setPrice(new BigDecimal("30.00"));

        return Arrays.asList(product1, product2, product3);
    }
} 