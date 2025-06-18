package com.cherniva.storefront.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;

import java.math.BigDecimal;

@Table("product")
@Data
public class Product {
    @Id
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;

    @Column("img_path")
    private String imgPath;
}
