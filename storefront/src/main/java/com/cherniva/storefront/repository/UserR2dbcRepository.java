package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserR2dbcRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String username);
}
