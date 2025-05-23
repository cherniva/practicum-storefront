package com.cherniva.storefront.utils;

import com.cherniva.storefront.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public class ProductUtils {
    public static BigDecimal getTotalAmount(List<Product> productsInCart) {
        return productsInCart.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static Mono<BigDecimal> getTotalAmount(Flux<Product> productsInCart) {
        return productsInCart
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
