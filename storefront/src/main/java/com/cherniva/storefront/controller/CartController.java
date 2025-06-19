package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.User;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import com.cherniva.storefront.repository.UserProductR2dbcRepository;
import com.cherniva.storefront.repository.UserR2dbcRepository;
import com.cherniva.storefront.service.PaymentService;
import com.cherniva.storefront.service.UserService;
import com.cherniva.storefront.utils.ProductUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
public class CartController {
    private final UserService userService;
    private final UserProductR2dbcRepository userProductRepository;
    private final ProductR2dbcRepository productRepository;
    private final ProductConverter productConverter;
    private final PaymentService paymentService;

    public CartController(UserService userService, UserProductR2dbcRepository userProductRepository,
                          ProductR2dbcRepository productRepository, ProductConverter productConverter, PaymentService paymentService) {
        this.userService = userService;
        this.userProductRepository = userProductRepository;
        this.productRepository = productRepository;
        this.productConverter = productConverter;
        this.paymentService = paymentService;
    }

    @GetMapping("/cart/products")
    public Mono<String> getCart(Model model) {
        Mono<Long> userIdMono = userService.getActiveUserIdMono();

        return userIdMono.flatMapMany(userProductRepository::findByUserId)
                .flatMap(userProduct -> productRepository.findById(userProduct.getProductId())
                        .map(product -> productConverter.productToProductDto(product, userProduct.getQuantity())))
                .collectList()
                .zipWith(paymentService.getBalance().onErrorResume(e -> Mono.just(-1.0)))
                .doOnNext(productsBalanceTuple -> {
                    List<ProductDto> productsDto = productsBalanceTuple.getT1();
                    Double balance = productsBalanceTuple.getT2();

                    BigDecimal totalAmount = ProductUtils.getTotalAmount(productsDto);
                    model.addAttribute("products", productsDto);
                    model.addAttribute("total", totalAmount);
                    model.addAttribute("empty", productsDto.isEmpty());
                    model.addAttribute("balance", balance);
                })
                .map(products -> "cart");
    }
}
