package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.CustomerOrderRepository;
import com.cherniva.storefront.repository.OrderProductRepo;
import com.cherniva.storefront.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerOrderRepository orderRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private OrderProductRepo orderProductRepo;

    @Test
    public void testPlaceOrder() throws Exception {
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
        when(productRepository.getProductsByCountGreaterThanZero()).thenReturn(productsInCart);
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(order);
        when(orderProductRepo.saveAll(any())).thenReturn(orderProducts);

        // Perform test
        mockMvc.perform(post("/buy"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(model().attribute("order", order));

        // Verify product counts are reset
        for (Product product : productsInCart) {
            when(productRepository.save(product)).thenReturn(product);
        }
    }

    @Test
    public void testGetOrders() throws Exception {
        CustomerOrder order1 = new CustomerOrder();
        order1.setId(1L);
        order1.setTotalSum(new BigDecimal("30.00"));

        CustomerOrder order2 = new CustomerOrder();
        order2.setId(2L);
        order2.setTotalSum(new BigDecimal("40.00"));

        List<CustomerOrder> orders = Arrays.asList(order1, order2);

        when(orderRepository.findAll()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", orders))
                .andExpect(model().attribute("numOrders", 2))
                .andExpect(model().attributeExists("totalSum"));
    }

    @Test
    public void testGetOrder() throws Exception {
        CustomerOrder order = new CustomerOrder();
        order.setId(1L);
        order.setTotalSum(new BigDecimal("30.00"));

        when(orderRepository.getReferenceById(1L)).thenReturn(order);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("newOrder", false))
                .andExpect(model().attribute("order", order));
    }
} 