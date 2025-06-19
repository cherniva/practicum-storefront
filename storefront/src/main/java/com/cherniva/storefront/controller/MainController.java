package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.User;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.UserProductR2dbcRepository;
import com.cherniva.storefront.repository.UserR2dbcRepository;
import com.cherniva.storefront.service.ProductService;
import com.cherniva.storefront.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Controller
@Slf4j
public class MainController {

    private final ProductService productService;
    private final ProductConverter productConverter;
    private final UserService userService;
    private final UserProductR2dbcRepository userProductRepository;

    public MainController(ProductService productService, ProductConverter productConverter,
                          UserService userService, UserProductR2dbcRepository userProductRepository) {
        this.productService = productService;
        this.productConverter = productConverter;
        this.userService = userService;
        this.userProductRepository = userProductRepository;
    }

    @GetMapping({"", "/", "/home", "/main", "/main/products"})
    public Mono<String> getProducts(Model model,
                                    @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                                    @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                    @RequestParam(name = "search", required = false) String search,
                                    @RequestParam(name = "sort", required = false) String sort) {
        System.out.println("[getProducts] Called with pageNumber=" + pageNumber + ", pageSize=" + pageSize + ", search=" + search + ", sort=" + sort);
        // Mono<Long> userIdMono = userService.getActiveUserIdMono();
        Mono<Long> userIdMono = userService.getActiveUserIdMono();

        String field = "ALPHA".equals(sort) ? "name" : "PRICE".equals(sort) ? "price" : "id";

        Mono<Page<Product>> productsPageMono = search != null
            ? productService.searchProductsByName(search, pageNumber-1, pageSize, field, "ASC")
            : productService.getProductsSortedBy(pageNumber-1, pageSize, field, "ASC");
        
        return Mono.zip(userIdMono, productsPageMono)
                .flatMap(tuple -> {
                    Long userId = tuple.getT1();
                    Page<Product> page = tuple.getT2();
                    List<Product> products = page.getContent();

                    // For each product, get UserProduct and map to DTO
                    return Flux.fromIterable(products)
                            .flatMap(product ->
                                    userProductRepository.findByUserIdAndProductId(userId, product.getId())
                                            .defaultIfEmpty(new UserProduct()) // If not found, return empty UserProduct
                                            .map(userProduct -> {
                                                int quantity = userProduct.getQuantity() == null ? 0 : userProduct.getQuantity();
                                                return productConverter.productToProductDto(product, quantity);
                                            })
                            )
                            .collectList()
                            .map(productDtos -> {
                                productDtos.sort(Comparator.comparing(ProductDto::getId));
                                int productsPerRow = 3;
                                List<List<ProductDto>> productRows = new ArrayList<>();
                                for (int i = 0; i < productDtos.size(); i += productsPerRow) {
                                    productRows.add(productDtos.subList(i, Math.min(i + productsPerRow, productDtos.size())));
                                }

                                model.addAttribute("products", productRows);
                                model.addAttribute("paging", new Paging(pageNumber, pageSize, page.getTotalPages()));
                                model.addAttribute("search", search);
                                model.addAttribute("sort", sort);
                                return "main";
                            });
                })
                .onErrorResume(e -> {
                    log.error("", e);
                    model.addAttribute("errorMessage", e.getMessage());
                    return Mono.just("error");
                });
    }

    private record Paging(int pageNumber, int pageSize, long totalPages) {
        public boolean hasPrevious() {
            return pageNumber > 1;
        }

        public boolean hasNext() {
            return pageNumber < totalPages;
        }
    }
}
