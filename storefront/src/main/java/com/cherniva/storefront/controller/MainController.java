package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

    private final ProductService productService;

    public MainController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"", "/", "/home", "/main", "/main/products"})
    public Mono<String> getProducts(Model model,
                                    @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                                    @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                    @RequestParam(name = "search", required = false) String search,
                                    @RequestParam(name = "sort", required = false) String sort) {
        String field = "ALPHA".equals(sort) ? "name" : "PRICE".equals(sort) ? "price" : "id";

        Mono<Page<Product>> productsPage;
        if (search != null)
            productsPage = productService.searchProductsByName(search, pageNumber-1, pageSize, field, "ASC");
        else
            productsPage = productService.getProductsSortedBy(pageNumber-1, pageSize, field, "ASC");

        return productsPage.doOnNext(page -> {
                    List<Product> products = page.getContent();

                    int productsPerRow = 3;
                    List<List<Product>> productRows = new ArrayList<>();

                    for (int i = 0; i < products.size(); i += productsPerRow) {
                        List<Product> row = new ArrayList<>();
                        for (int j = 0; j < productsPerRow && (i + j) < products.size(); j++) {
                            row.add(products.get(i + j));
                        }
                        productRows.add(row);
                    }

                    model.addAttribute("products", productRows);
                    model.addAttribute("paging", new Paging(pageNumber, pageSize, page.getTotalPages()));
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                })
                .map(page -> "main");
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
