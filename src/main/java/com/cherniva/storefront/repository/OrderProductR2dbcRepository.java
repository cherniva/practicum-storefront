package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.OrderProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrderProductR2dbcRepository extends ReactiveCrudRepository<OrderProduct, Long> {
    Flux<OrderProduct> findByOrderId(Long orderId);
}
