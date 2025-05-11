package com.cherniva.storefront.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    private String imgPath;
    private Integer count;
    @ManyToMany(mappedBy = "addedProducts", fetch = FetchType.LAZY)
    private List<Cart> carts;
    @ManyToMany(mappedBy = "orderedProducts", fetch = FetchType.LAZY)
    private List<CustomerOrder> orders;
}
