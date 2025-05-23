package com.cherniva.storefront.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Table("customer_order")
@Data
@ToString(exclude = "products")
public class CustomerOrder {
    @Id
    private Long id;

    @Column("total_sum")
    private BigDecimal totalSum;

    // Note: R2DBC doesn't support @OneToMany relationships directly
    // You'll need to handle this relationship manually in your service layer
    // This field is for convenience but won't be automatically populated
    private transient List<OrderProduct> products;
}
