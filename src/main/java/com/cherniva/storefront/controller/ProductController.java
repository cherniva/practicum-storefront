package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products/{id}")
    public String getProduct(Model model,
                             @PathVariable("id") Long id) {
        Product product = productRepository.getReferenceById(id);

        model.addAttribute("product", product);

        return "product";
    }

    @PostMapping({"/products/{id}", "/main/products/{id}"})
    public String addToCart(Model model,
                            @PathVariable("id") Long id,
                            @RequestParam("action") String action,
                            @RequestHeader(value = "referer", required = false) String referer) {
        Product product = productRepository.getReferenceById(id);

        if ("plus".equals(action)) {
            product.setCount(product.getCount() + 1);
        } else if ("minus".equals(action)) {
            product.setCount(Math.max(0, product.getCount() - 1));
        }

        productRepository.save(product);

        return "redirect:" + (referer != null ? referer : "/products/" + id);
    }
}
