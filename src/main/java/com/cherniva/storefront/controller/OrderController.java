package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.OrderProductR2dbcRepository;
import com.cherniva.storefront.repository.CustomerOrderR2dbcRepository;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.utils.OrderUtils;
import com.cherniva.storefront.utils.ProductUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class OrderController {
    private final CustomerOrderR2dbcRepository orderRepository;
    private final ProductR2dbcRepository productRepository;
    private final OrderProductR2dbcRepository orderProductRepo;

    public OrderController(CustomerOrderR2dbcRepository orderRepository, ProductR2dbcRepository productRepository, OrderProductR2dbcRepository orderProductRepo) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderProductRepo = orderProductRepo;
    }

    @PostMapping("/buy")
    public Mono<String> placeOrder(Model model) {
        Flux<Product> productsFlux = productRepository.getProductsByCountGreaterThanZero();

        // Calculate total in reactive way
        Mono<BigDecimal> totalAmountMono = ProductUtils.getTotalAmount(productsFlux);

        // Create order with calculated total
        Mono<CustomerOrder> savedOrderMono = totalAmountMono
                .flatMap(totalAmount -> {
                    CustomerOrder order = new CustomerOrder();
                    order.setTotalSum(totalAmount);
                    return orderRepository.save(order);
                });

        // Process everything together
        return Mono.zip(productsFlux.collectList(), savedOrderMono)
                .flatMap(tuple -> {
                    List<Product> products = tuple.getT1();
                    CustomerOrder savedOrder = tuple.getT2();

                    // Save order products and reset counts in parallel
                    Mono<List<OrderProduct>> orderProductsMono = Flux.fromIterable(products)
                            .map(p -> productToOrderProduct(p, savedOrder))
                            .collectList()
                            .flatMap(orderProducts -> orderProductRepo.saveAll(orderProducts).collectList());

                    Mono<Void> resetCountsMono = Flux.fromIterable(products)
                            .map(product -> {
                                product.setCount(0);
                                return product;
                            })
                            .flatMap(productRepository::save)
                            .then();

                    return Mono.zip(orderProductsMono, resetCountsMono)
                            .map(result -> {
                                savedOrder.setProducts(result.getT1());
                                return savedOrder;
                            });
                })
                .doOnNext(savedOrder -> {
                    model.addAttribute("newOrder", true);
                    model.addAttribute("order", savedOrder);
                })
                .map(savedOrder -> "order")
                .onErrorReturn("error");
    }

    @GetMapping("/orders")
    public Mono<String> getOrders(Model model) {
        return orderRepository.findAll()
                .collectList()
                .doOnNext(orders -> {
                    model.addAttribute("orders", orders);
                    model.addAttribute("numOrders", orders.size());
                    model.addAttribute("totalSum", OrderUtils.getTotalSum(orders));
                })
                .map(orders -> "orders");
    }

    @GetMapping("/orders/{id}")
    public Mono<String> getOrder(Model model,
                                 @PathVariable("id") Long id) {
        return orderRepository.findById(id)
                .doOnNext(order -> {
                    model.addAttribute("newOrder", false);
                    model.addAttribute("order", order);
                })
                .map(order -> "order");
    }

    private OrderProduct productToOrderProduct(Product product, CustomerOrder order) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setProduct(product);
        orderProduct.setOrder(order);
        orderProduct.setQuantity(product.getCount());
        return orderProduct;
    }
}
