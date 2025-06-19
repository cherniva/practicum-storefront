package com.cherniva.storefront.controller;

import com.cherniva.storefront.repository.UserR2dbcRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

@Controller
public class LoginController {

    private final UserR2dbcRepository userRepository;

    public LoginController(UserR2dbcRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public Mono<String> login(@RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "logout", required = false) String logout,
                             Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully.");
        }
        return Mono.just("login");
    }

    @GetMapping("/debug/user")
    @ResponseBody
    public Mono<String> debugUser() {
        return userRepository.findByUsername("admin")
                .map(user -> "User found: " + user.getUsername() + ", Password: " + user.getPassword())
                .defaultIfEmpty("User not found");
    }
} 