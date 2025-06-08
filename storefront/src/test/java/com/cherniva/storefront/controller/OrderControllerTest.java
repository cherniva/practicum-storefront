package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.CustomerOrderR2dbcRepository;
import com.cherniva.storefront.repository.OrderProductR2dbcRepository;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CustomerOrderR2dbcRepository orderRepository;

    @MockBean
    private ProductR2dbcRepository productRepository;

    @MockBean
    private OrderProductR2dbcRepository orderProductRepo;
    @MockBean
    private PaymentService paymentService;

    @Test
    public void testPlaceOrder() {
        // Create test products
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setCount(2);
        product1.setPrice(new BigDecimal("10.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setCount(1);
        product2.setPrice(new BigDecimal("20.00"));

        List<Product> productsInCart = Arrays.asList(product1, product2);

        // Create order products
        OrderProduct orderProduct1 = new OrderProduct();
        orderProduct1.setProduct(product1);
        orderProduct1.setQuantity(2);

        OrderProduct orderProduct2 = new OrderProduct();
        orderProduct2.setProduct(product2);
        orderProduct2.setQuantity(1);

        List<OrderProduct> orderProducts = Arrays.asList(orderProduct1, orderProduct2);

        // Create and setup order
        CustomerOrder order = new CustomerOrder();
        order.setId(1L);
        order.setTotalSum(new BigDecimal("40.00"));
        order.setProducts(orderProducts);

        // Setup mocks
        when(productRepository.getProductsByCountGreaterThanZero()).thenReturn(Flux.fromIterable(productsInCart));
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(Mono.just(order));
        when(orderProductRepo.saveAll(any(Iterable.class))).thenReturn(Flux.fromIterable(orderProducts));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product1));
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
    public void testGetOrders() {
        CustomerOrder order1 = new CustomerOrder();
        order1.setId(1L);
        order1.setTotalSum(new BigDecimal("30.00"));

        CustomerOrder order2 = new CustomerOrder();
        order2.setId(2L);
        order2.setTotalSum(new BigDecimal("40.00"));

        List<CustomerOrder> orders = Arrays.asList(order1, order2);

        when(orderRepository.findAll()).thenReturn(Flux.fromIterable(orders));
        when(paymentService.processPayment(anyDouble())).thenReturn(Mono.just(1000.0));

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
    public void testGetOrder() {
        // Create test order
        CustomerOrder order = new CustomerOrder();
        order.setId(1L);
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
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(paymentService.processPayment(anyDouble())).thenReturn(Mono.just(1000.0));

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
} 