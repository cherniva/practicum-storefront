package com.cherniva.storefront.service;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ProductPageService {

    private final ProductR2dbcRepository productRepository;

    public ProductPageService(ProductR2dbcRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Get paginated products with dynamic sorting based on field name
     */
    public Mono<Page<Product>> getProductsSortedBy(int page, int size, String field, String direction) {
        System.out.println("NO CACHE");
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(field).ascending() :
                Sort.by(field).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAllBy(pageable)
                .collectList()
                .zipWith(productRepository.count())
                .map(t -> new PageImpl<>(t.getT1(), PageRequest.of(page, size), t.getT2()));
    }

    /**
     * Search products by name with pagination and sorting
     */
    public Mono<Page<Product>> searchProductsByName(String keyword, int page, int size,
                                              String sortField, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortField).ascending() :
                Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .collectList()
                .zipWith(productRepository.count())
                .map(t -> new PageImpl<>(t.getT1(), PageRequest.of(page, size), t.getT2()));
    }
}