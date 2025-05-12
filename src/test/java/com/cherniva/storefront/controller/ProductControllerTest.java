package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @Test
    public void testGetProduct() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));

        when(productRepository.getReferenceById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("product"))
                .andExpect(model().attribute("product", product));
    }

    @Test
    public void testAddToCartPlus() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setCount(0);

        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        mockMvc.perform(post("/products/1")
                .param("action", "plus")
                .header("referer", "/products/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/1"));
    }

    @Test
    public void testGetNewProductForm() throws Exception {
        mockMvc.perform(get("/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-product.html"));
    }

    @Test
    public void testAddNewProduct() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("New Product");
        savedProduct.setDescription("New Description");
        savedProduct.setPrice(new BigDecimal("149.99"));

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        mockMvc.perform(multipart("/products/new")
                .file(imageFile)
                .param("name", "New Product")
                .param("description", "New Description")
                .param("price", "149.99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/products"));
    }
} 