package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.CustomerOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerOrderR2dbcRepository extends ReactiveCrudRepository<CustomerOrder, Long> {
    Flux<CustomerOrder> findByUserId(Long userId);
}
