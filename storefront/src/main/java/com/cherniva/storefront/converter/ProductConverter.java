package com.cherniva.storefront.converter;

import com.cherniva.storefront.dto.ProductDto;
import com.cherniva.storefront.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductConverter {
    public Product productDtoToProduct(ProductDto productDto) {
        Product product = new Product();
        product.setId(productDto.getId());
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setImgPath(productDto.getImgPath());

        return product;
    }

    public ProductDto productToProductDto(Product product, Integer quantity) {
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setImgPath(product.getImgPath());
        productDto.setCount(quantity);

        return productDto;
    }
}
