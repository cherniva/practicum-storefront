package com.cherniva.storefront.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
@Data
public class UserProduct {
    @Id
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
