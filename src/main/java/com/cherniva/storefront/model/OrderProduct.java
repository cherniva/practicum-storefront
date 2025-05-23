package com.cherniva.storefront.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;
import lombok.ToString;

@Table("order_product")
@Data
@ToString(exclude = "order")
public class OrderProduct {
    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("product_id")
    private Long productId;

    private Integer quantity;

    // Transient fields for convenience - not mapped to database
    private transient CustomerOrder order;
    private transient Product product;
}

