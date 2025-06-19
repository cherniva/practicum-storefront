package com.cherniva.storefront.controller;

import com.cherniva.storefront.converter.ProductConverter;
import com.cherniva.storefront.model.Product;
import com.cherniva.storefront.model.UserProduct;
import com.cherniva.storefront.repository.UserProductR2dbcRepository;
import com.cherniva.storefront.service.ProductService;
import com.cherniva.storefront.service.UserService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.codec.multipart.Part;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;

@Controller
@Slf4j
public class ProductController {
    private final ProductService productService;
    private final ProductConverter productConverter;
    private final UserService userService;
    private final UserProductR2dbcRepository userProductRepository;

    public ProductController(ProductService productService, ProductConverter productConverter,
                             UserService userService, UserProductR2dbcRepository userProductRepository) {
        this.productService = productService;
        this.productConverter = productConverter;
        this.userService = userService;
        this.userProductRepository = userProductRepository;
    }

    @GetMapping("/products/{id}")
    public Mono<String> getProduct(@PathVariable("id") Long id, Model model) {
        Mono<Long> userIdMono = userService.getActiveUserIdMono();

        return Mono.zip(userIdMono, Mono.just(id))
                .flatMap(tuple -> userProductRepository.findByUserIdAndProductId(tuple.getT1(), tuple.getT2())
                        .switchIfEmpty(Mono.defer(() -> {
                            // If no UserProduct exists, create a default one with quantity 0
                            UserProduct defaultUserProduct = new UserProduct();
                            defaultUserProduct.setUserId(tuple.getT1());
                            defaultUserProduct.setProductId(tuple.getT2());
                            defaultUserProduct.setQuantity(0);
                            return Mono.just(defaultUserProduct);
                        })))
                .flatMap(userProduct -> productService.findById(userProduct.getProductId())
                        .map(product -> productConverter.productToProductDto(product, userProduct.getQuantity())))
                .doOnNext(productDto -> model.addAttribute("product", productDto))
                .map(productDto -> "product");
    }

    @PostMapping({"/products/{id}", "/main/products/{id}", "/cart/products/{id}"})
    public Mono<String> addToCart(@PathVariable("id") Long id,
                                  ServerWebExchange exchange) {
        Mono<Long> userIdMono = userService.getActiveUserIdMono();

        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    String requestPath = exchange.getRequest().getPath().value();
                    log.info("Received addToCart request - id: {}, action: {}, path: {}", id, action, requestPath);

                    return userIdMono.flatMap(userId -> userProductRepository.findByUserIdAndProductId(userId, id)
                                    .switchIfEmpty(
                                            Mono.defer(() -> {
                                                UserProduct userProduct = getDefaultUserProduct(userId, id);
                                                return userProductRepository.save(userProduct);
                                            })
                                    )
                                    .flatMap(userProduct -> {
                                        int newQuantity = switch (action) {
                                            case "plus" -> userProduct.getQuantity() + 1;
                                            case "minus" -> userProduct.getQuantity() - 1;
                                            case "delete" -> 0;
                                            default -> throw new IllegalStateException("Unexpected value: " + action);
                                        };

                                        if (newQuantity <= 0) {
                                            return userProductRepository.delete(userProduct).then(Mono.just(getDefaultUserProduct(userId, id)));
                                        } else {
                                            userProduct.setQuantity(newQuantity);
                                            return userProductRepository.save(userProduct);
                                        }
                                    })
                            )
                            .flatMap(userProduct -> productService.findById(userProduct.getProductId())
                                    .map(product -> productConverter.productToProductDto(product, userProduct.getQuantity())))
                            .map(productDto -> {
                                String redirectUrl = determineRedirectUrl(requestPath, id);
                                log.info("Redirecting to: {}", redirectUrl);
                                return "redirect:" + redirectUrl;
                            })
                            .onErrorResume(e -> {
                                log.error("Error in addToCart: ", e);
                                String redirectUrl = determineRedirectUrl(requestPath, id);
                                return Mono.just("redirect:" + redirectUrl);
                            });
                });
    }

    private String determineRedirectUrl(String requestPath, Long productId) {
        if (requestPath.startsWith("/main/products/")) {
            return "/main/products";
        } else if (requestPath.startsWith("/cart/products/")) {
            return "/cart/products";
        } else if (requestPath.startsWith("/products/")) {
            return "/products/" + productId;
        } else {
            // Fallback to product detail page
            return "/products/" + productId;
        }
    }

    private UserProduct getDefaultUserProduct(Long userId, Long productId) {
        UserProduct userProduct = new UserProduct();
        userProduct.setUserId(userId);
        userProduct.setProductId(productId);
        userProduct.setQuantity(0);

        return userProduct;
    }

    @GetMapping("products/new")
    public Mono<String> getNewProductForm() {
        return Mono.just("add-product");
    }

    @PostMapping(value = "products/new", consumes = "multipart/form-data")
    @Transactional
    public Mono<String> addNewProduct(ServerWebExchange exchange, Model model) {
        log.info("Starting new product creation process");
        return exchange.getMultipartData()
                .flatMap(multipartData -> {
                    log.info("Received multipart data with parts: {}", multipartData.keySet());
                    Part imagePart = multipartData.getFirst("image");
                    if (imagePart == null) {
                        log.warn("No image file provided in the request");
                        model.addAttribute("error", "Please select an image file");
                        return Mono.just("add-product");
                    }

                    log.info("Image part received with content type: {}", imagePart.headers().getContentType());
                    return Mono.fromCallable(() -> {
                        log.info("Creating new product from form data");
                        Product product = new Product();
                        
                        return Flux.zip(
                            multipartData.getFirst("name").content().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                String content = new String(bytes);
                                log.debug("Extracted name content: {}", content);
                                return content;
                            }),
                            multipartData.getFirst("description").content().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                String content = new String(bytes);
                                log.debug("Extracted description content: {}", content);
                                return content;
                            }),
                            multipartData.getFirst("price").content().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                String content = new String(bytes);
                                log.debug("Extracted price content: {}", content);
                                return content;
                            })
                        ).next().flatMap(tuple -> {
                            String name = tuple.getT1();
                            String description = tuple.getT2();
                            String price = tuple.getT3();
                            
                            log.info("Product details - Name: {}, Description length: {}, Price: {}",
                                name, description.length(), price);
                            
                            product.setName(name);
                            product.setDescription(description);
                            product.setPrice(new BigDecimal(price));

                            return saveImageFile(product.getName(), imagePart)
                                .map(imagePath -> {
                                    log.info("Image saved successfully at path: {}", imagePath);
                                    product.setImgPath("uploads/" + imagePath.getFileName().toString());
                                    return product;
                                });
                        });
                    })
                    .flatMap(mono -> mono)
                    .flatMap(product -> {
                        log.info("Saving product to database: {}", product);
                        return productService.save(product);
                    })
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(savedProduct -> {
                        log.info("Product saved successfully with ID: {}", savedProduct.getId());
                        log.info("Cache cleared for all product-related entries");
                    })
                    .delayElement(Duration.ofMillis(500)) // Add a small delay to ensure file operations are completed
                    .map(savedProduct -> {
                        log.info("Redirecting to main products page with fresh data");
                        return "redirect:/main/products";
                    })
                    .onErrorResume(e -> {
                        log.error("Error creating product: ", e);
                        model.addAttribute("error", "Error creating product: " + e.getMessage());
                        return Mono.just("add-product");
                    });
                });
    }

    private Mono<Path> saveImageFile(String name, Part imagePart) {
        log.info("Starting image file save process for product: {}", name);
        
        // Validate content type
        String contentType = imagePart.headers().getContentType().toString();
        log.info("Image content type: {}", contentType);
        
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            log.error("Invalid content type: {}", contentType);
            return Mono.error(new IllegalArgumentException("Only PNG or JPEG images are allowed."));
        }

        // Get upload directory from environment variable or use default
        String uploadDir = System.getenv("UPLOAD_DIR");
        if (uploadDir == null) {
            uploadDir = "uploads";
            log.info("Using default upload directory: {}", uploadDir);
        } else {
            log.info("Using custom upload directory from environment: {}", uploadDir);
        }

        // Create directory if it doesn't exist
        Path uploadPath = Path.of(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(uploadPath);
            log.info("Ensured upload directory exists at: {}", uploadPath);
        } catch (IOException e) {
            return Mono.error(e);
        }

        // Sanitize filename
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = name.replaceAll("[^a-zA-Z0-9.-]", "_") + extension;
        log.info("Generated filename: {}", filename);

        // Save file
        Path filePath = uploadPath.resolve(filename);
        log.info("Full file path for saving: {}", filePath);
        
        return Mono.fromCallable(() -> Files.createTempFile("upload_", extension))
            .flatMap(tempFile -> {
                log.info("Created temporary file: {}", tempFile);
                return imagePart.content()
                    .reduce(new ArrayList<byte[]>(), (list, dataBuffer) -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        list.add(bytes);
                        return list;
                    })
                    .flatMap(byteArrays -> {
                        // Calculate total size
                        int totalSize = byteArrays.stream()
                            .mapToInt(bytes -> bytes.length)
                            .sum();
                        
                        // Combine all byte arrays
                        byte[] allBytes = new byte[totalSize];
                        int currentPos = 0;
                        for (byte[] bytes : byteArrays) {
                            System.arraycopy(bytes, 0, allBytes, currentPos, bytes.length);
                            currentPos += bytes.length;
                        }
                        
                        return Mono.fromCallable(() -> {
                            Files.write(tempFile, allBytes);
                            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                            log.info("Successfully moved file from {} to {}", tempFile, filePath);
                            return filePath;
                        }).doFinally(signalType -> {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ex) {
                                log.warn("Failed to delete temporary file: {}", tempFile, ex);
                            }
                        });
                    });
            })
            .onErrorResume(e -> {
                log.error("Error saving image file: ", e);
                return Mono.error(e);
            });
    }
}
