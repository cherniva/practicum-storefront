package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.UserProductR2dbcRepository;
import com.cherniva.storefront.service.ProductService;
import com.cherniva.storefront.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(MainController.class)
@Import(MainControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.keycloak.client-id=test",
    "spring.security.oauth2.client.registration.keycloak.client-secret=test"
})
public class MainControllerTest {

    @EnableWebFluxSecurity
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public ReactiveUserDetailsService userDetailsService() {
            UserDetails user = User.withUsername("testuser")
                    .password("password")
                    .authorities(new SimpleGrantedAuthority("USER"))
                    .build();
            return username -> Mono.just(user);
        }

        @org.springframework.context.annotation.Bean
        public org.springframework.security.web.server.SecurityWebFilterChain securityWebFilterChain(
                org.springframework.security.config.web.server.ServerHttpSecurity http) {
            return http
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers("", "/", "/home", "/main", "/main/products").permitAll()
                            .anyExchange().authenticated()
                    )
                    .oauth2Login(org.springframework.security.config.Customizer.withDefaults())
                    .csrf(org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec::disable)
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductConverter productConverter;

    @MockBean
    private UserService userService;

    @MockBean
    private UserProductR2dbcRepository userProductRepository;

    @Test
    public void testGetProductsDefault() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        // Mock UserService to return -1L for anonymous user
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(-1L));

        // Mock ProductService
        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        // Mock UserProductRepository to return empty for anonymous user (-1L)
        for (Product product : products) {
            when(userProductRepository.findByUserIdAndProductId(-1L, product.getId()))
                    .thenReturn(Mono.empty());
        }

        // Mock ProductConverter for each product with count 0
        for (Product product : products) {
            ProductDto productDto = new ProductDto();
            productDto.setId(product.getId());
            productDto.setName(product.getName());
            productDto.setPrice(product.getPrice());
            productDto.setCount(0);
            when(productConverter.productToProductDto(product, 0)).thenReturn(productDto);
        }

        webTestClient.get()
                .uri("/main/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                    assertTrue(body.contains("Product 2"), "Response should contain Product 2");
                    assertTrue(body.contains("Product 3"), "Response should contain Product 3");
                    assertTrue(body.contains("Страница: 1"), "Response should show page number");
                });
    }

    @Test
    public void testGetProductsWithSearch() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        // Mock UserService to return -1L for anonymous user
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(-1L));

        // Mock ProductService
        when(productService.searchProductsByName(anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        // Mock UserProductRepository to return empty for anonymous user (-1L)
        for (Product product : products) {
            when(userProductRepository.findByUserIdAndProductId(-1L, product.getId()))
                    .thenReturn(Mono.empty());
        }

        // Mock ProductConverter for each product with count 0
        for (Product product : products) {
            ProductDto productDto = new ProductDto();
            productDto.setId(product.getId());
            productDto.setName(product.getName());
            productDto.setPrice(product.getPrice());
            productDto.setCount(0);
            when(productConverter.productToProductDto(product, 0)).thenReturn(productDto);
        }

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/products")
                        .queryParam("search", "test")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("test"), "Response should contain search term");
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                });
    }

    @Test
    public void testGetProductsWithSort() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        // Mock UserService to return -1L for anonymous user
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(-1L));

        // Mock ProductService
        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        // Mock UserProductRepository to return empty for anonymous user (-1L)
        for (Product product : products) {
            when(userProductRepository.findByUserIdAndProductId(-1L, product.getId()))
                    .thenReturn(Mono.empty());
        }

        // Mock ProductConverter for each product with count 0
        for (Product product : products) {
            ProductDto productDto = new ProductDto();
            productDto.setId(product.getId());
            productDto.setName(product.getName());
            productDto.setPrice(product.getPrice());
            productDto.setCount(0);
            when(productConverter.productToProductDto(product, 0)).thenReturn(productDto);
        }

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/products")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("PRICE"), "Response should contain sort parameter");
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                });
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetProductsAuthenticated() {
        List<Product> products = createTestProducts();
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), products.size());

        // Mock UserService to return a real user ID for authenticated user
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L));

        // Mock ProductService
        when(productService.getProductsSortedBy(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(productPage));

        // Mock UserProductRepository for authenticated user
        for (Product product : products) {
            UserProduct userProduct = new UserProduct();
            userProduct.setId(product.getId());
            userProduct.setUserId(999L);
            userProduct.setProductId(product.getId());
            userProduct.setQuantity(2);
            when(userProductRepository.findByUserIdAndProductId(999L, product.getId()))
                    .thenReturn(Mono.just(userProduct));
        }

        // Mock ProductConverter for each product with count 2
        for (Product product : products) {
            ProductDto productDto = new ProductDto();
            productDto.setId(product.getId());
            productDto.setName(product.getName());
            productDto.setPrice(product.getPrice());
            productDto.setCount(2);
            when(productConverter.productToProductDto(product, 2)).thenReturn(productDto);
        }

        webTestClient.get()
                .uri("/main/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                    assertTrue(body.contains("Product 2"), "Response should contain Product 2");
                    assertTrue(body.contains("Product 3"), "Response should contain Product 3");
                });
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