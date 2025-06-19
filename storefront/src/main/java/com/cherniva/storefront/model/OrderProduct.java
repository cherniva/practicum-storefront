package com.cherniva.storefront.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;
import lombok.ToString;

@Table("order_product")
@Data
public class OrderProduct {
    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("product_id")
    private Long productId;

    private Integer quantity;

    @Transient
    private Product product;
}

