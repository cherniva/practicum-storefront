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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.io.ByteArrayResource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@WebFluxTest(ProductController.class)
@Import(ProductControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.keycloak.client-id=test",
    "spring.security.oauth2.client.registration.keycloak.client-secret=test"
})
public class ProductControllerTest {

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
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));

        UserProduct userProduct = new UserProduct();
        userProduct.setId(1L);
        userProduct.setUserId(999L);
        userProduct.setProductId(1L);
        userProduct.setQuantity(2);

        ProductDto productDto = new ProductDto();
        productDto.setId(1L);
        productDto.setName("Test Product");
        productDto.setDescription("Test Description");
        productDto.setPrice(new BigDecimal("99.99"));
        productDto.setCount(2);

        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L));
        when(userProductRepository.findByUserIdAndProductId(999L, 1L)).thenReturn(Mono.just(userProduct));
        when(productService.findById(1L)).thenReturn(Mono.just(product));
        when(productConverter.productToProductDto(product, 2)).thenReturn(productDto);

        webTestClient.get()
                .uri("/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Test Product"), "Response should contain product name");
                    assertTrue(body.contains("Test Description"), "Response should contain product description");
                    assertTrue(body.contains("99.99"), "Response should contain product price");
                });
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testAddToCartPlus() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));

        UserProduct userProduct = new UserProduct();
        userProduct.setId(1L);
        userProduct.setUserId(999L);
        userProduct.setProductId(1L);
        userProduct.setQuantity(1);

        ProductDto productDto = new ProductDto();
        productDto.setId(1L);
        productDto.setName("Test Product");
        productDto.setPrice(new BigDecimal("99.99"));
        productDto.setCount(1);

        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L));
        when(userProductRepository.findByUserIdAndProductId(999L, 1L)).thenReturn(Mono.just(userProduct));
        when(userProductRepository.save(any(UserProduct.class))).thenReturn(Mono.just(userProduct));
        when(productService.findById(1L)).thenReturn(Mono.just(product));
        when(productConverter.productToProductDto(product, 1)).thenReturn(productDto);

        webTestClient.post()
                .uri("/products/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=plus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/products/1");
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetNewProductForm() {
        webTestClient.get()
                .uri("/products/new")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Добавить новый товар"), "Response should contain form title");
                    assertTrue(body.contains("name"), "Response should contain name field");
                    assertTrue(body.contains("description"), "Response should contain description field");
                    assertTrue(body.contains("price"), "Response should contain price field");
                });
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testAddNewProduct() {
        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("New Product");
        savedProduct.setDescription("New Description");
        savedProduct.setPrice(new BigDecimal("149.99"));
        savedProduct.setImgPath("uploads/test-image.png");

        when(productService.save(any(Product.class))).thenReturn(Mono.just(savedProduct));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("name", "New Product");
        builder.part("description", "New Description");
        builder.part("price", "149.99");

        ByteArrayResource imageResource = new ByteArrayResource(new byte[]{1, 2, 3, 4}) {
            @Override
            public String getFilename() {
                return "test-image.png";
            }
        };
        builder.part("image", imageResource, MediaType.IMAGE_PNG);

        webTestClient.post()
                .uri("/products/new")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/products");
    }

    @Test
    public void testGetProductWithoutAuthentication() {
        webTestClient.get()
                .uri("/products/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }

    @Test
    public void testAddToCartWithoutAuthentication() {
        webTestClient.post()
                .uri("/products/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=plus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }

    @Test
    public void testGetNewProductFormWithoutAuthentication() {
        webTestClient.get()
                .uri("/products/new")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }

    @Test
    public void testAddNewProductWithoutAuthentication() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("name", "New Product");
        builder.part("description", "New Description");
        builder.part("price", "149.99");

        ByteArrayResource imageResource = new ByteArrayResource(new byte[]{1, 2, 3, 4}) {
            @Override
            public String getFilename() {
                return "test-image.png";
            }
        };
        builder.part("image", imageResource, MediaType.IMAGE_PNG);

        webTestClient.post()
                .uri("/products/new")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }
}