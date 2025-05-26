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
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
        logger.info("Starting new product creation process");
        return exchange.getMultipartData()
                .flatMap(multipartData -> {
                    logger.info("Received multipart data with parts: {}", multipartData.keySet());
                    Part imagePart = multipartData.getFirst("image");
                    if (imagePart == null) {
                        logger.warn("No image file provided in the request");
                        model.addAttribute("error", "Please select an image file");
                        return Mono.just("add-product");
                    }

                    logger.info("Image part received with content type: {}", imagePart.headers().getContentType());
                    return Mono.fromCallable(() -> {
                        logger.info("Creating new product from form data");
                        Product product = new Product();
                        
                        return Flux.zip(
                            multipartData.getFirst("name").content().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                String content = new String(bytes);
                                logger.debug("Extracted name content: {}", content);
                                return content;
                            }),
                            multipartData.getFirst("description").content().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                String content = new String(bytes);
                                logger.debug("Extracted description content: {}", content);
                                return content;
                            }),
                            multipartData.getFirst("price").content().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                String content = new String(bytes);
                                logger.debug("Extracted price content: {}", content);
                                return content;
                            })
                        ).next().flatMap(tuple -> {
                            String name = tuple.getT1();
                            String description = tuple.getT2();
                            String price = tuple.getT3();
                            
                            logger.info("Product details - Name: {}, Description length: {}, Price: {}", 
                                name, description.length(), price);
                            
                            product.setName(name);
                            product.setDescription(description);
                            product.setPrice(new BigDecimal(price));
                            product.setCount(0);

                            return saveImageFile(product.getName(), imagePart)
                                .map(imagePath -> {
                                    logger.info("Image saved successfully at path: {}", imagePath);
                                    product.setImgPath("uploads/" + imagePath.getFileName().toString());
                                    return product;
                                });
                        });
                    })
                    .flatMap(mono -> mono)
                    .flatMap(product -> {
                        logger.info("Saving product to database: {}", product);
                        return productRepository.save(product);
                    })
                    .publishOn(Schedulers.boundedElastic())
                    .doOnSuccess(savedProduct -> {
                        logger.info("Product saved successfully with ID: {}", savedProduct.getId());
                    })
                    .delayElement(Duration.ofMillis(500)) // Add a small delay to ensure file operations are completed
                    .map(savedProduct -> {
                        logger.info("Redirecting to main products page");
                        return "redirect:/main/products";
                    })
                    .onErrorResume(e -> {
                        logger.error("Error creating product: ", e);
                        model.addAttribute("error", "Error creating product: " + e.getMessage());
                        return Mono.just("add-product");
                    });
                });
    }

    private Mono<Path> saveImageFile(String name, Part imagePart) {
        logger.info("Starting image file save process for product: {}", name);
        
        // Validate content type
        String contentType = imagePart.headers().getContentType().toString();
        logger.info("Image content type: {}", contentType);
        
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            logger.error("Invalid content type: {}", contentType);
            return Mono.error(new IllegalArgumentException("Only PNG or JPEG images are allowed."));
        }

        // Get upload directory from environment variable or use default
        String uploadDir = System.getenv("UPLOAD_DIR");
        if (uploadDir == null) {
            uploadDir = "src/main/resources/static/uploads";
            logger.info("Using default upload directory: {}", uploadDir);
        } else {
            logger.info("Using custom upload directory from environment: {}", uploadDir);
        }

        // Create directory if it doesn't exist
        Path uploadPath = Path.of(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            logger.info("Ensured upload directory exists at: {}", uploadPath);
        } catch (IOException e) {
            return Mono.error(e);
        }

        // Sanitize filename
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = name + extension;
        logger.info("Generated filename: {}", filename);

        // Save file
        Path filePath = uploadPath.resolve(filename);
        logger.info("Full file path for saving: {}", filePath);
        
        return Mono.fromCallable(() -> Files.createTempFile("upload_", extension))
            .flatMap(tempFile -> {
                logger.info("Created temporary file: {}", tempFile);
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
                            logger.info("Successfully moved file from {} to {}", tempFile, filePath);
                            return filePath;
                        }).doFinally(signalType -> {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ex) {
                                logger.warn("Failed to delete temporary file: {}", tempFile, ex);
                            }
                        });
                    });
            })
            .onErrorResume(e -> {
                logger.error("Error saving image file: ", e);
                return Mono.error(e);
            });
    }
}
