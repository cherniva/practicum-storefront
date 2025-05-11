package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MainController {

    private ProductService productService;

    public MainController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"", "/", "/home"})
    public String getProducts(Model model,
                              @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                              @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                              @RequestParam(name = "search", required = false) String search,
                              @RequestParam(name = "sort", required = false) String sort) {
        String field = "ALPHA".equals(sort) ? "name" : "PRICE".equals(sort) ? "price" : "id";
        Page<Product> products;
        if (search != null)
            products = productService.searchProductsByName(search, pageNumber, pageSize, field, "ASC");
        else
            products = productService.getProductsSortedBy(pageNumber, pageSize, field, "ASC");

        model.addAttribute("products", products.get().toList());
        model.addAttribute("paging", new Paging(pageNumber, pageSize, products.getTotalPages()));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        return "main";
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
