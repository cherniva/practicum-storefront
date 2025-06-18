package com.cherniva.storefront.service;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class ProductService {

    private final ProductR2dbcRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final int ttl;

    public ProductService(ProductR2dbcRepository productRepository, RedisTemplate<String, Object> redisTemplate,
                          @Value("${redis.template.ttl:1}") int ttl) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    /**
     * Get paginated products with dynamic sorting based on field name
     */
    public Mono<Page<Product>> getProductsSortedBy(int page, int size, String field, String direction) {
        String cacheKey = String.format("products:sorted:%d:%d:%s:%s", 
            page, size, field, direction);

        // Try to get from cache first
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            if (cachedData instanceof Page) {
                return Mono.just((Page<Product>) cachedData);
            } else if (cachedData instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) cachedData;
                List<Map<String, Object>> content = (List<Map<String, Object>>) map.get("content");
                List<Product> products = new ArrayList<>();
                for (Map<String, Object> item : content) {
                    Product product = new Product();
                    product.setId(((Number) item.get("id")).longValue());
                    product.setName((String) item.get("name"));
                    product.setDescription((String) item.get("description"));
                    product.setPrice(BigDecimal.valueOf(((Number) item.get("price")).doubleValue()));
                    product.setImgPath((String) item.get("imgPath"));
                    products.add(product);
                }
                Pageable pageable = PageRequest.of(page, size);
                return Mono.just(new PageImpl<>(products, pageable, ((Number) map.get("totalElements")).longValue()));
            }
        }

        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(field).ascending() :
                Sort.by(field).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAllBy(pageable)
                .collectList()
                .zipWith(productRepository.count())
                .map(t -> {
                    Page<Product> pageResult = new PageImpl<>(t.getT1(), PageRequest.of(page, size), t.getT2());
                    // Cache the result
                    redisTemplate.opsForValue().set(cacheKey, pageResult, ttl, TimeUnit.MINUTES);
                    return pageResult;
                });
    }

    /**
     * Search products by name with pagination and sorting
     */
    public Mono<Page<Product>> searchProductsByName(String keyword, int page, int size,
                                              String sortField, String direction) {
        String cacheKey = String.format("products:search:%s:%d:%d:%s:%s",
            keyword, page, size, sortField, direction);

        // Try to get from cache first
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            if (cachedData instanceof Page) {
                return Mono.just((Page<Product>) cachedData);
            } else if (cachedData instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) cachedData;
                List<Map<String, Object>> content = (List<Map<String, Object>>) map.get("content");
                List<Product> products = new ArrayList<>();
                for (Map<String, Object> item : content) {
                    Product product = new Product();
                    product.setId(((Number) item.get("id")).longValue());
                    product.setName((String) item.get("name"));
                    product.setDescription((String) item.get("description"));
                    product.setPrice(BigDecimal.valueOf(((Number) item.get("price")).doubleValue()));
                    product.setImgPath((String) item.get("imgPath"));
                    products.add(product);
                }
                Pageable pageable = PageRequest.of(page, size);
                return Mono.just(new PageImpl<>(products, pageable, ((Number) map.get("totalElements")).longValue()));
            }
        }

        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortField).ascending() :
                Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .collectList()
                .zipWith(productRepository.count())
                .map(t -> {
                    Page<Product> pageResult = new PageImpl<>(t.getT1(), PageRequest.of(page, size), t.getT2());
                    // Cache the result
                    redisTemplate.opsForValue().set(cacheKey, pageResult, ttl, TimeUnit.MINUTES);
                    return pageResult;
                });
    }

    public Mono<Product> findById(Long id) {
        String cacheKey = String.format("product:%d", id);
        
        // Try to get from cache first
        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            if (cachedProduct instanceof Product) {
                return Mono.just((Product) cachedProduct);
            } else if (cachedProduct instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) cachedProduct;
                Product product = new Product();
                product.setId(((Number) map.get("id")).longValue());
                product.setName((String) map.get("name"));
                product.setDescription((String) map.get("description"));
                product.setPrice(BigDecimal.valueOf(((Number) map.get("price")).doubleValue()));
                product.setImgPath((String) map.get("imgPath"));
                return Mono.just(product);
            }
        }

        // If not in cache, get from database
        return productRepository.findById(id)
                .doOnNext(product -> redisTemplate.opsForValue().set(cacheKey, product, ttl, TimeUnit.MINUTES));
    }

    public Mono<Product> save(Product product) {
        return productRepository.save(product)
            .doOnNext(savedProduct -> {
                // Clear all product-related caches
                redisTemplate.delete(redisTemplate.keys("products:*"));
                redisTemplate.delete(redisTemplate.keys("product:*"));
            });
    }

    public void clearCache() {
        redisTemplate.delete(redisTemplate.keys("products:*"));
        redisTemplate.delete(redisTemplate.keys("product:*"));
    }
}