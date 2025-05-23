package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Controller
public class ProductController {
    private final ProductR2dbcRepository productRepository;

    public ProductController(ProductR2dbcRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products/{id}")
    public Mono<String> getProductReactive(Model model,
                                           @PathVariable("id") Long id) {
        return productRepository.findById(id)
                .doOnNext(product -> model.addAttribute("product", product))
                .map(product -> "product");
    }

    @PostMapping({"/products/{id}", "/main/products/{id}", "/cart/products/{id}"})
    public Mono<String> addToCartReactive(Model model,
                                  @PathVariable("id") Long id,
                                  @RequestParam("action") String action,
                                  @RequestHeader(value = "referer", required = false) String referer) {
        return productRepository.findById(id)
                .flatMap(product -> {
                    Integer count = switch (action) {
                        case "plus" -> product.getCount() + 1;
                        case "minus" -> Math.max(0, product.getCount() - 1);
                        case "delete" -> 0;
                        default -> throw new IllegalStateException("Unexpected value: " + action);
                    };

                    product.setCount(count);

                    return productRepository.save(product);
                })
                .map(product -> "redirect:" + (referer != null ? referer : "/products/" + id));
    }

    @GetMapping("products/new")
    public Mono<String> getNewProductForm(Model model) {
        return Mono.just("add-product");
    }

    @PostMapping(value = "products/new", consumes = "multipart/form-data")
    @Transactional
    public Mono<String> addNewProduct(Model model,
                                      @RequestParam String name, @RequestParam String description,
                                      @RequestParam BigDecimal price, @RequestParam("image") MultipartFile imageFile) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);

        Path imagePath;
        try {
            imagePath = saveImageFile(name, imageFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Store relative path for database
        product.setImgPath("/uploads/" + imagePath.getFileName().toString());

        productRepository.save(product);

        return Mono.just("redirect:/main/products");
    }

    private Path saveImageFile(String name, MultipartFile imageFile) throws IOException {
        // Validate content type
        String contentType = imageFile.getContentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            throw new IllegalArgumentException("Only PNG or JPEG images are allowed.");
        }

        // Get upload directory from environment variable or use default
        String uploadDir = System.getenv("UPLOAD_DIR");
        if (uploadDir == null) {
            uploadDir = "src/main/resources/static/uploads";
        }

        // Create directory if it doesn't exist
        Path uploadPath = Path.of(uploadDir);
        Files.createDirectories(uploadPath);

        // Sanitize filename
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = name + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }
}
