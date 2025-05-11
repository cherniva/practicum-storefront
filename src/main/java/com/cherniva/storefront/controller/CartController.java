package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController {
    private final ProductRepository productRepository;

    public CartController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/cart/products")
    public String getCart(Model model) {
        List<Product> productsInCart = productRepository.getProductsByCountGreaterThanZero();
        BigDecimal totalAmount = getTotalAmount(productsInCart);

        model.addAttribute("products", productsInCart);
        model.addAttribute("total", totalAmount);
        model.addAttribute("empty", productsInCart.isEmpty());

        return "cart";
    }

    private BigDecimal getTotalAmount(List<Product> productsInCart) {
        return productsInCart.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
