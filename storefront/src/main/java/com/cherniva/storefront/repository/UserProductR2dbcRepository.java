package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.UserProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserProductR2dbcRepository extends ReactiveCrudRepository<UserProduct, Long> {
    Flux<UserProduct> findByUserId(Long userId);
    Flux<UserProduct> findByProductId(Long productId);
    Mono<UserProduct> findByUserIdAndProductId(Long userId, Long productId);
}
