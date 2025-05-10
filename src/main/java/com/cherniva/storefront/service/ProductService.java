package com.cherniva.storefront.service;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Get paginated products with default sorting
     */
    public Page<Product> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable);
    }

    /**
     * Get paginated products with sorting by price (ascending or descending)
     */
    public Page<Product> getProductsSortedByPrice(int page, int size, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by("price").ascending() :
                Sort.by("price").descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable);
    }

    /**
     * Get paginated products with sorting by name (ascending or descending)
     */
    public Page<Product> getProductsSortedByName(int page, int size, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by("name").ascending() :
                Sort.by("name").descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable);
    }

    /**
     * Get paginated products with compound sorting (price and name)
     */
    public Page<Product> getProductsSortedByPriceAndName(int page, int size,
                                                         String priceDirection,
                                                         String nameDirection) {
        List<Sort.Order> orders = new ArrayList<>();

        // Add price sorting
        if (priceDirection.equalsIgnoreCase("asc")) {
            orders.add(Sort.Order.asc("price"));
        } else {
            orders.add(Sort.Order.desc("price"));
        }

        // Add name sorting (secondary)
        if (nameDirection.equalsIgnoreCase("asc")) {
            orders.add(Sort.Order.asc("name"));
        } else {
            orders.add(Sort.Order.desc("name"));
        }

        // Create pageable with multiple sort criteria
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        return productRepository.findAll(pageable);
    }

    /**
     * Get paginated products with dynamic sorting based on field name
     */
    public Page<Product> getProductsSortedBy(int page, int size, String field, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(field).ascending() :
                Sort.by(field).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable);
    }

    /**
     * Search products by name with pagination and sorting
     */
    public Page<Product> searchProductsByName(String keyword, int page, int size,
                                              String sortField, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortField).ascending() :
                Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    /**
     * Get products by price range with pagination and sorting
     */
    public Page<Product> getProductsByPriceRange(Double minPrice, Double maxPrice,
                                                 int page, int size,
                                                 String sortField, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortField).ascending() :
                Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
    }
}