package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.CustomerOrderR2dbcRepository;
import com.cherniva.storefront.repository.OrderProductR2dbcRepository;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.repository.UserProductR2dbcRepository;
import com.cherniva.storefront.service.PaymentService;
import com.cherniva.storefront.service.ProductService;
import com.cherniva.storefront.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
@Import(OrderControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.keycloak.client-id=test",
    "spring.security.oauth2.client.registration.keycloak.client-secret=test"
})
public class OrderControllerTest {

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
    private CustomerOrderR2dbcRepository orderRepository;

    @MockBean
    private ProductR2dbcRepository productRepository;

    @MockBean
    private OrderProductR2dbcRepository orderProductRepo;

    @MockBean
    private UserProductR2dbcRepository userProductRepository;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private ProductService productService;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductConverter productConverter;

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testPlaceOrder() {
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
        userProduct1.setUserId(999L);
        userProduct1.setProductId(1L);
        userProduct1.setQuantity(2);

        UserProduct userProduct2 = new UserProduct();
        userProduct2.setId(2L);
        userProduct2.setUserId(999L);
        userProduct2.setProductId(2L);
        userProduct2.setQuantity(1);

        List<UserProduct> userProducts = Arrays.asList(userProduct1, userProduct2);

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

        List<ProductDto> productDtos = Arrays.asList(productDto1, productDto2);

        // Create order products
        OrderProduct orderProduct1 = new OrderProduct();
        orderProduct1.setId(1L);
        orderProduct1.setOrderId(1L);
        orderProduct1.setProductId(1L);
        orderProduct1.setQuantity(2);
        orderProduct1.setProduct(product1);

        OrderProduct orderProduct2 = new OrderProduct();
        orderProduct2.setId(2L);
        orderProduct2.setOrderId(1L);
        orderProduct2.setProductId(2L);
        orderProduct2.setQuantity(1);
        orderProduct2.setProduct(product2);

        List<OrderProduct> orderProducts = Arrays.asList(orderProduct1, orderProduct2);

        // Create and setup order
        CustomerOrder order = new CustomerOrder();
        order.setId(1L);
        order.setUserId(999L);
        order.setTotalSum(new BigDecimal("40.00"));
        order.setProducts(orderProducts);

        // Setup mocks
        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L));
        when(userProductRepository.findByUserId(999L)).thenReturn(Flux.fromIterable(userProducts));
        when(productRepository.findById(1L)).thenReturn(Mono.just(product1));
        when(productRepository.findById(2L)).thenReturn(Mono.just(product2));
        when(productConverter.productToProductDto(product1, 2)).thenReturn(productDto1);
        when(productConverter.productToProductDto(product2, 1)).thenReturn(productDto2);
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(Mono.just(order));
        when(orderProductRepo.saveAll(any(Iterable.class))).thenReturn(Flux.fromIterable(orderProducts));
        when(userProductRepository.findByUserIdAndProductId(anyLong(), anyLong()))
                .thenReturn(Mono.just(userProduct1))
                .thenReturn(Mono.just(userProduct2));
        when(userProductRepository.delete(any(UserProduct.class))).thenReturn(Mono.empty());
        when(productService.findById(1L)).thenReturn(Mono.just(product1));
        when(productService.findById(2L)).thenReturn(Mono.just(product2));
        when(paymentService.processPayment(anyDouble())).thenReturn(Mono.just(1000.0));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Поздравляем! Успешная покупка!"), "Response should contain success message");
                    assertTrue(body.contains("Заказ №1"), "Response should contain order number");
                    assertTrue(body.contains("40.00"), "Response should contain total sum");
                });
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetOrders() {
        CustomerOrder order1 = new CustomerOrder();
        order1.setId(1L);
        order1.setUserId(999L);
        order1.setTotalSum(new BigDecimal("30.00"));

        CustomerOrder order2 = new CustomerOrder();
        order2.setId(2L);
        order2.setUserId(999L);
        order2.setTotalSum(new BigDecimal("40.00"));

        List<CustomerOrder> orders = Arrays.asList(order1, order2);

        when(userService.getActiveUserIdMono()).thenReturn(Mono.just(999L));
        when(orderRepository.findByUserId(999L)).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("2 заказов"), "Response should show number of orders");
                    assertTrue(body.contains("Заказ №1"), "Response should contain first order");
                    assertTrue(body.contains("Заказ №2"), "Response should contain second order");
                });
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testGetOrder() {
        // Create test order
        CustomerOrder order = new CustomerOrder();
        order.setId(1L);
        order.setUserId(999L);
        order.setTotalSum(new BigDecimal("30.00"));

        // Create test order products
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setId(1L);
        orderProduct.setOrderId(1L);
        orderProduct.setProductId(1L);
        orderProduct.setQuantity(2);

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("15.00"));

        // Setup mocks
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderProductRepo.findByOrderId(1L)).thenReturn(Flux.just(orderProduct));
        when(productService.findById(1L)).thenReturn(Mono.just(product));

        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    assertTrue(body.contains("Заказ №1"), "Response should contain order number");
                    assertTrue(body.contains("30.00"), "Response should contain order total");
                    assertTrue(body.contains("Test Product"), "Response should contain product name");
                });
    }

    @Test
    public void testPlaceOrderWithoutAuthentication() {
        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    public void testGetOrdersWithoutAuthentication() {
        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }

    @Test
    public void testGetOrderWithoutAuthentication() {
        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/oauth2/authorization/keycloak");
    }
}