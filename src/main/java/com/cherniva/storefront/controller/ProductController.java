package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductRepository;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Controller
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products/{id}")
    public String getProduct(Model model,
                             @PathVariable("id") Long id) {
        Product product = productRepository.getReferenceById(id);

        model.addAttribute("product", product);

        return "product";
    }

    @PostMapping({"/products/{id}", "/main/products/{id}", "/cart/products/{id}"})
    public String addToCart(Model model,
                            @PathVariable("id") Long id,
                            @RequestParam("action") String action,
                            @RequestHeader(value = "referer", required = false) String referer) {
        Product product = productRepository.getReferenceById(id);

        Integer count = switch (action) {
            case "plus" -> product.getCount() + 1;
            case "minus" -> Math.max(0, product.getCount() - 1);
            case "delete" -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + action);
        };

        product.setCount(count);

        productRepository.save(product);

        return "redirect:" + (referer != null ? referer : "/products/" + id);
    }

    @GetMapping("products/new")
    public String getNewProductForm(Model model) {
        return "add-product.html";
    }

    @PostMapping(value = "products/new", consumes = "multipart/form-data")
    @Transactional
    public String addNewProduct(Model model,
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

        product.setImgPath(imagePath.toString());

        productRepository.save(product);

        return "redirect:/main/products";
    }

    private Path saveImageFile(String name, MultipartFile imageFile) throws IOException {
        // Validate content type (optional but recommended)
        String contentType = imageFile.getContentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            throw new IllegalArgumentException("Only PNG or JPEG images are allowed.");
        }

        // Define upload path
        Path baseDir = Path.of("src/main/resources/static");
        Path uploadDir = Path.of("uploads");

        // Sanitize filename (e.g. replace spaces, restrict characters)
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = name + extension;

        // Save file
        Path filePath = uploadDir.resolve(filename);
        Files.copy(imageFile.getInputStream(), baseDir.resolve(filePath), StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }
}
