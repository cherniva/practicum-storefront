package com.cherniva.storefront.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String imgPath;
    private String count;
    @ManyToMany(mappedBy = "addedProducts", fetch = FetchType.LAZY)
    private List<Cart> carts;
    @ManyToMany(mappedBy = "orderedProducts", fetch = FetchType.LAZY)
    private List<CustomerOrder> orders;
}
