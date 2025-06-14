package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface ProductR2dbcRepository extends R2dbcRepository<Product, Long> {
    Flux<Product> getByName(String name);
    @Query("SELECT * FROM product WHERE count IS NOT NULL AND count > 0")
    Flux<Product> getProductsByCountGreaterThanZero();
    Flux<Product> findAllBy(Pageable pageable);
    Flux<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
