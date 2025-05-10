package com.cherniva.storefront.repository;

import com.cherniva.storefront.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
