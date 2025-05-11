package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> getByName(String name);
    @Query("SELECT p FROM Product p WHERE p.count IS NOT NULL AND p.count > 0")
    List<Product> getProductsByCountGreaterThanZero();
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);
}
