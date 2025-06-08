package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.service.PaymentService;
import com.cherniva.storefront.utils.ProductUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController {
    private final ProductR2dbcRepository productRepository;
    private final PaymentService paymentService;

    public CartController(ProductR2dbcRepository productRepository, PaymentService paymentService) {
        this.productRepository = productRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/cart/products")
    public Mono<String> getCart(Model model) {
        return productRepository.getProductsByCountGreaterThanZero()
                .collectList()
                .zipWith(paymentService.getBalance().onErrorResume(e -> Mono.just(-1.0)))
                .doOnNext(productsBalanceTuple -> {
                    List<Product> products = productsBalanceTuple.getT1();
                    Double balance = productsBalanceTuple.getT2();

                    BigDecimal totalAmount = ProductUtils.getTotalAmount(products);
                    model.addAttribute("products", products);
                    model.addAttribute("total", totalAmount);
                    model.addAttribute("empty", products.isEmpty());
                    model.addAttribute("balance", balance);
                })
                .map(products -> "cart");
    }
}
