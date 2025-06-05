package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.CustomerOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerOrderR2dbcRepository extends ReactiveCrudRepository<CustomerOrder, Long> {
}
