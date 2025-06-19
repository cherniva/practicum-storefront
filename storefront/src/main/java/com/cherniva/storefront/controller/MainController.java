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
import java.util.List;

@Controller
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
        Mono<Long> userIdMono = userService.getActiveUserIdMono()
            .doOnNext(id -> System.out.println("[getProducts] userIdMono emitted: " + id))
            .doOnError(e -> System.out.println("[getProducts] userIdMono error: " + e.getMessage()));

        String field = "ALPHA".equals(sort) ? "name" : "PRICE".equals(sort) ? "price" : "id";

        // Mono<Page<Product>> productsPageMono;
        // if (search != null)
        //     productsPageMono = productService.searchProductsByName(search, pageNumber-1, pageSize, field, "ASC");
        // else
        //     productsPageMono = productService.getProductsSortedBy(pageNumber-1, pageSize, field, "ASC");
        Mono<Page<Product>> productsPageMono = (search != null
            ? productService.searchProductsByName(search, pageNumber-1, pageSize, field, "ASC")
            : productService.getProductsSortedBy(pageNumber-1, pageSize, field, "ASC"))
            .doOnNext(page -> System.out.println("[getProducts] productsPageMono emitted: " + page))
            .doOnError(e -> System.out.println("[getProducts] productsPageMono error: " + e.getMessage()));
        
        return Mono.zip(userIdMono, productsPageMono)
                .flatMap(tuple -> {
                    Long userId = tuple.getT1();
                    Page<Product> page = tuple.getT2();
                    List<Product> products = page.getContent();
                    System.out.println("[getProducts] userId=" + userId + ", products.size=" + products.size());

                    // For each product, get UserProduct and map to DTO
                    return Flux.fromIterable(products)
                            .flatMap(product ->
                                    userProductRepository.findByUserIdAndProductId(userId, product.getId())
                                            .defaultIfEmpty(new UserProduct()) // If not found, return empty UserProduct
                                            .map(userProduct -> {
                                                int quantity = userProduct.getQuantity();
                                                ProductDto dto = productConverter.productToProductDto(product, quantity);
                                                dto.setCount(userProduct.getQuantity() != null ? userProduct.getQuantity() : 0);
                                                return dto;
                                            })
                            )
                            .collectList()
                            .map(productDtos -> {
                                System.out.println("[getProducts] productDtos.size=" + productDtos.size());
                                // 6. Optionally chunk into rows for the view
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
                    System.out.println("[getProducts] ERROR: " + e.getMessage());
                    e.printStackTrace();
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
