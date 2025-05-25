package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.OrderProductR2dbcRepository;
import com.cherniva.storefront.repository.CustomerOrderR2dbcRepository;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.utils.OrderUtils;
import com.cherniva.storefront.utils.ProductUtils;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
                    log.info("Processing order {} with {} products", savedOrder.getId(), products.size());

                    // Create and save order products
                    return Flux.fromIterable(products)
                            .map(p -> {
                                OrderProduct op = productToOrderProduct(p, savedOrder);
                                log.info("Creating order product for product {} with quantity {}", p.getId(), op.getQuantity());
                                return op;
                            })
                            .collectList()
                            .flatMap(orderProducts -> {
                                log.info("Saving {} order products", orderProducts.size());
                                return orderProductRepo.saveAll(orderProducts)
                                    .collectList()
                                    .flatMap(savedOrderProducts -> {
                                        log.info("Successfully saved {} order products", savedOrderProducts.size());
                                        savedOrder.setProducts(savedOrderProducts);
                                        return orderRepository.save(savedOrder)
                                            .doOnSuccess(order -> log.info("Updated order {} with products", order.getId()));
                                    });
                            })
                            .then(Mono.just(savedOrder));
                })
                .flatMap(savedOrder -> {
                    model.addAttribute("newOrder", true);
                    model.addAttribute("order", savedOrder);
                    return Mono.just("buy");
                })
                .onErrorResume(e -> {
                    log.error("Error processing order: ", e);
                    return Mono.just("error");
                });
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
        log.info("Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .doOnNext(order -> log.info("Found order: id={}, totalSum={}", order.getId(), order.getTotalSum()))
                .flatMap(order -> 
                    orderProductRepo.findByOrderId(order.getId())
                        .doOnNext(op -> log.info("Found order product: id={}, productId={}, quantity={}", 
                            op.getId(), op.getProductId(), op.getQuantity()))
                        .flatMap(orderProduct -> 
                            productRepository.findById(orderProduct.getProductId())
                                .doOnNext(product -> {
                                    log.info("Found product: id={}, name={}", product.getId(), product.getName());
                                    orderProduct.setProduct(product);
                                })
                                .thenReturn(orderProduct)
                        )
                        .collectList()
                        .doOnNext(products -> log.info("Collected {} order products for order {}", products.size(), order.getId()))
                        .map(orderProducts -> {
                            order.setProducts(orderProducts);
                            log.info("Set {} products to order {}", orderProducts.size(), order.getId());
                            return order;
                        })
                )
                .doOnNext(order -> {
                    log.info("Adding order to model: id={}, productsCount={}", 
                        order.getId(), order.getProducts() != null ? order.getProducts().size() : 0);
                    model.addAttribute("newOrder", false);
                    model.addAttribute("order", order);
                })
                .map(order -> "buy");
    }

    private OrderProduct productToOrderProduct(Product product, CustomerOrder order) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setProduct(product);
        orderProduct.setOrder(order);
        orderProduct.setQuantity(product.getCount());
        orderProduct.setOrderId(order.getId());
        orderProduct.setProductId(product.getId());
        log.info("Created OrderProduct: productId={}, orderId={}, quantity={}", 
            product.getId(), order.getId(), product.getCount());
        return orderProduct;
    }
}
