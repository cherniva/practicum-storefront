package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.CustomerOrder;
import com.cherniva.storefront.model.OrderProduct;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.*;
import com.cherniva.storefront.service.PaymentService;
import com.cherniva.storefront.service.ProductService;
import com.cherniva.storefront.service.UserService;
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
    private final UserService userService;
    private final UserProductR2dbcRepository userProductRepository;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final ProductConverter productConverter;

    public OrderController(CustomerOrderR2dbcRepository orderRepository, ProductR2dbcRepository productRepository,
                           OrderProductR2dbcRepository orderProductRepo, UserService userService,
                           UserProductR2dbcRepository userProductRepository, PaymentService paymentService,
                           ProductService productService, ProductConverter productConverter) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderProductRepo = orderProductRepo;
        this.userService = userService;
        this.userProductRepository = userProductRepository;
        this.paymentService = paymentService;
        this.productService = productService;
        this.productConverter = productConverter;
    }

    @PostMapping("/buy")
    public Mono<String> placeOrder(Model model) {
        Mono<Long> userIdMono = userService.getActiveUserIdMono();

        return userIdMono.flatMap(userId -> {
            Flux<UserProduct> userProductFlux = userProductRepository.findByUserId(userId);
            return userProductFlux.flatMap(userProduct -> productRepository.findById(userProduct.getProductId())
                    .map(product -> productConverter.productToProductDto(product, userProduct.getQuantity())))
                    .collectList()
                    .flatMap(productDtos -> {
                        BigDecimal totalAmount = ProductUtils.getTotalAmount(productDtos);
                        CustomerOrder order = new CustomerOrder();
                        order.setUserId(userId);
                        order.setTotalSum(totalAmount);
                        return Mono.zip(Mono.just(productDtos), orderRepository.save(order));
                    })
                    .flatMap(tuple -> {
                        List<ProductDto> products = tuple.getT1();
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
                                .then(Mono.just(savedOrder))
                                .flatMap(order ->
                                        // Process payment first
                                        paymentService.processPayment(order.getTotalSum().doubleValue())
                                                .doOnNext(balance -> log.info("Payment processed successfully. New balance: {}", balance))
                                                .thenReturn(order)
                                )
                                .flatMap(order ->
                                        // Reset product counts and save
                                        Flux.fromIterable(products)
                                                .flatMap(productDto -> userProductRepository.findByUserIdAndProductId(userId, productDto.getId())
                                                        .flatMap(userProduct -> userProductRepository.delete(userProduct)))
                                                .then()
                                                .doOnSuccess(unused -> log.info("Deleted user-product associations for {} products", products.size()))
                                                .thenReturn(order)
                                );
                    })
                    .flatMap(savedOrder -> {
                        productService.clearCache();
                        model.addAttribute("newOrder", true);
                        model.addAttribute("order", savedOrder);
                        return Mono.just("buy");
                    })
                    .onErrorResume(e -> {
                        log.error("Error processing order: ", e);
                        return Mono.just("error");
                    });
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

    private OrderProduct productToOrderProduct(ProductDto productDto, CustomerOrder order) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setQuantity(productDto.getCount());
        orderProduct.setOrderId(order.getId());
        orderProduct.setProductId(productDto.getId());
        log.info("Created OrderProduct: productId={}, orderId={}, quantity={}",
                productDto.getId(), order.getId(), productDto.getCount());
        return orderProduct;
    }
}
