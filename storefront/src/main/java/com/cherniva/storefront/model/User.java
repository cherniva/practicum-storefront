package com.cherniva.storefront.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("user")
@Data
public class User {
    @Id
    private Long id;
    private String username;
    private String password;
}
