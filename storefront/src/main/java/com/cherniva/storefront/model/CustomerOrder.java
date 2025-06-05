package com.cherniva.storefront.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Table("customer_order")
@Data
public class CustomerOrder {
    @Id
    private Long id;

    @Column("total_sum")
    private BigDecimal totalSum;

    // This field is not mapped to the database
    // It's used for convenience in the application layer
    @Transient
    private List<OrderProduct> products = new ArrayList<>();
}
