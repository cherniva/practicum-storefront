package com.cherniva.storefront.controller;

import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.repository.ProductR2dbcRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.codec.multipart.Part;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;

@Controller
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductR2dbcRepository productRepository;

    public ProductController(ProductR2dbcRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products/{id}")
    public Mono<String> getProduct(@PathVariable("id") Long id, Model model) {
        return productRepository.findById(id)
                .doOnNext(product -> model.addAttribute("product", product))
                .map(product -> "product");
    }

    @PostMapping({"/products/{id}", "/main/products/{id}", "/cart/products/{id}"})
    public Mono<String> addToCart(@PathVariable("id") Long id,
                                  ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    String referer = exchange.getRequest().getHeaders().getFirst("referer");
                    
                    logger.info("Received addToCart request - id: {}, action: {}, referer: {}", id, action, referer);
                    
                    return productRepository.findById(id)
                            .switchIfEmpty(Mono.error(new RuntimeException("Product not found with id: " + id)))
                            .flatMap(product -> {
                                logger.info("Found product: {}", product);
                                Integer count = switch (action) {
                                    case "plus" -> product.getCount() + 1;
                                    case "minus" -> Math.max(0, product.getCount() - 1);
                                    case "delete" -> 0;
                                    default -> throw new IllegalStateException("Unexpected value: " + action);
                                };

                                product.setCount(count);
                                return productRepository.save(product);
                            })
                            .map(product -> {
                                String redirectUrl = referer != null ? referer : "/products/" + id;
                                logger.info("Redirecting to: {}", redirectUrl);
                                return "redirect:" + redirectUrl;
                            })
                            .onErrorResume(e -> {
                                logger.error("Error in addToCart: ", e);
                                String redirectUrl = referer != null ? referer : "/products/" + id;
                                return Mono.just("redirect:" + redirectUrl);
                            });
                });
    }

    @GetMapping("products/new")
    public Mono<String> getNewProductForm() {
        return Mono.just("add-product");
    }

    @PostMapping(value = "products/new", consumes = "multipart/form-data")
    @Transactional
    public Mono<String> addNewProduct(ServerWebExchange exchange, Model model) {
        return exchange.getMultipartData()
                .flatMap(multipartData -> {
                    Part imagePart = multipartData.getFirst("image");
                    if (imagePart == null) {
                        model.addAttribute("error", "Please select an image file");
                        return Mono.just("add-product");
                    }

                    return Mono.fromCallable(() -> {
                        Product product = new Product();
                        product.setName(multipartData.getFirst("name").content().toString());
                        product.setDescription(multipartData.getFirst("description").content().toString());
                        product.setPrice(new BigDecimal(multipartData.getFirst("price").content().toString()));
                        product.setCount(0);

                        Path imagePath = saveImageFile(product.getName(), imagePart);
                        product.setImgPath("/uploads/" + imagePath.getFileName().toString());
                        return product;
                    })
                    .flatMap(product -> productRepository.save(product))
                    .map(savedProduct -> "redirect:/main/products")
                    .onErrorResume(e -> {
                        model.addAttribute("error", "Error creating product: " + e.getMessage());
                        return Mono.just("add-product");
                    });
                });
    }

    private Path saveImageFile(String name, Part imagePart) throws IOException {
        // Validate content type
        String contentType = imagePart.headers().getContentType().toString();
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
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(filePath, 
            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        
        Flux<DataBuffer> content = imagePart.content();
        content.subscribe(dataBuffer -> {
            ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
            channel.write(byteBuffer, 0);
        });

        return filePath;
    }
}
