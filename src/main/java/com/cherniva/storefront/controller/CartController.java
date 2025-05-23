package com.cherniva.storefront.controller;

import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.utils.ProductUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Controller
public class CartController {
    private final ProductR2dbcRepository productRepository;

    public CartController(ProductR2dbcRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/cart/products")
    public Mono<String> getCart(Model model) {
        return productRepository.getProductsByCountGreaterThanZero()
                .collectList()
                .doOnNext(products -> {
                    BigDecimal totalAmount = ProductUtils.getTotalAmount(products);
                    model.addAttribute("products", products);
                    model.addAttribute("total", totalAmount);
                    model.addAttribute("empty", products.isEmpty());
                })
                .map(products -> "cart");
    }
}
