package com.cherniva.storefront.service;

import com.cherniva.storefront.model.User;
import com.cherniva.storefront.repository.UserR2dbcRepository;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserService {
    private final UserR2dbcRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserR2dbcRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<User> getActiveUserMono() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    // Check if this is an OAuth2 authentication
                    if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                        // Try to get email first, then fallback to name (or sub -> subjectId)
                        String email = oauth2User.getAttribute("email");

                        if (email != null) {
                            return userRepository.findByUsername(email)
                                    .switchIfEmpty(createUserFromOAuth2(oauth2User));
                        } else {
                            // Fallback to OAuth2 subject ID
                            String subjectId = oauth2User.getName();
                            return userRepository.findByUsername(subjectId)
                                    .switchIfEmpty(createUserFromOAuth2(oauth2User));
                        }
                    } else {
                        // Traditional authentication
                        String username = authentication.getName();
                        System.out.println("Traditional auth username: " + username);
                        return userRepository.findByUsername(username);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    User anonymousUser = new User();
                    anonymousUser.setId(-1L);
                    return Mono.just(anonymousUser);
                }));
    }

    public Mono<Long> getActiveUserIdMono() {
        return getActiveUserMono().map(User::getId);
    }

    private Mono<User> createUserFromOAuth2(OAuth2User oauth2User) {
        // Create a new user from OAuth2 information
        User newUser = new User();

        // Try to get email first
        String email = oauth2User.getAttribute("email");
        if (email != null) {
            newUser.setUsername(email);
        } else {
            // Fallback to subjectId
            newUser.setUsername(oauth2User.getName());
        }

        // Set a placeholder password (you might want to handle this differently)
        String randomPassword = generateRandomPassword();
        newUser.setPassword(passwordEncoder.encode(randomPassword));

        // Save the new user to database
        return userRepository.save(newUser);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return password.toString();
    }
}
