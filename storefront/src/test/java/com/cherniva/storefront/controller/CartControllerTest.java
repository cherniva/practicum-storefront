package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.repository.UserProductR2dbcRepository;
import com.cherniva.storefront.repository.UserR2dbcRepository;
import com.cherniva.storefront.service.PaymentService;
import com.cherniva.storefront.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@WebFluxTest(CartController.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.keycloak.client-id=test",
    "spring.security.oauth2.client.registration.keycloak.client-secret=test"
})
public class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductR2dbcRepository productRepository;
    @MockBean
    private UserProductR2dbcRepository userProductRepository;
    @MockBean
    private UserR2dbcRepository userRepository;
    @MockBean
    private PaymentService paymentService;
    @MockBean
    private UserService userService;
    @MockBean
    private ProductConverter productConverter;

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetCart() {
        // Create test products
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("10.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("20.00"));

        // Create test user products
        UserProduct userProduct1 = new UserProduct();
        userProduct1.setId(1L);
        userProduct1.setUserId(999L); // Use a different user ID to avoid conflicts
        userProduct1.setProductId(1L);
        userProduct1.setQuantity(2);

        UserProduct userProduct2 = new UserProduct();
        userProduct2.setId(2L);
        userProduct2.setUserId(999L); // Use a different user ID to avoid conflicts
        userProduct2.setProductId(2L);
        userProduct2.setQuantity(1);

        // Create test product DTOs
        ProductDto productDto1 = new ProductDto();
        productDto1.setId(1L);
        productDto1.setName("Product 1");
        productDto1.setPrice(new BigDecimal("10.00"));
        productDto1.setCount(2);

        ProductDto productDto2 = new ProductDto();
        productDto2.setId(2L);
        productDto2.setName("Product 2");
        productDto2.setPrice(new BigDecimal("20.00"));
        productDto2.setCount(1);

        List<UserProduct> userProducts = Arrays.asList(userProduct1, userProduct2);
        List<ProductDto> productDtos = Arrays.asList(productDto1, productDto2);

        // Mock the service calls
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L)); // Use different user ID
        when(userProductRepository.findByUserId(999L)).thenReturn(Flux.fromIterable(userProducts));
        when(productRepository.findById(1L)).thenReturn(Mono.just(product1));
        when(productRepository.findById(2L)).thenReturn(Mono.just(product2));
        when(productConverter.productToProductDto(product1, 2)).thenReturn(productDto1);
        when(productConverter.productToProductDto(product2, 1)).thenReturn(productDto2);
        when(paymentService.getBalance()).thenReturn(Mono.just(1000.0));

        webTestClient.get()
                .uri("/cart/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Product 1"), "Response should contain Product 1");
                    assertTrue(body.contains("Product 2"), "Response should contain Product 2");
                    assertTrue(body.contains("40.00"), "Response should contain total amount of 40.00");
                    assertTrue(body.contains("<button>Купить</button>"), "Response should contain Buy button");
                });
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetEmptyCart() {
        // Mock the service calls
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L)); // Use different user ID
        when(userProductRepository.findByUserId(999L)).thenReturn(Flux.empty());
        when(paymentService.getBalance()).thenReturn(Mono.just(1000.0));

        webTestClient.get()
                .uri("/cart/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertFalse(body.contains("<button>Купить</button>"), "Response should not contain Buy button for empty cart");
                });
    }

    @Test
    public void testGetCartWithoutAuthentication() {
        webTestClient.get()
                .uri("/cart/products")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }
}