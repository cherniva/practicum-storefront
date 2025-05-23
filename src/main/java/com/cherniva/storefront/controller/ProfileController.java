package com.cherniva.storefront.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ProfileController {

    @Autowired
    private Environment environment;

    @GetMapping("/profile")
    public Mono<String> getActiveProfile() {
        return Mono.just("Active profiles: " + String.join(", ", environment.getActiveProfiles()));
    }
} 