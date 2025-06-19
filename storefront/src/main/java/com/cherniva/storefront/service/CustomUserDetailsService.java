package com.cherniva.storefront.service;

import com.cherniva.storefront.model.User;
import com.cherniva.storefront.repository.UserR2dbcRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserR2dbcRepository userRepository;

    public CustomUserDetailsService(UserR2dbcRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        System.out.println("[CustomUserDetailsService] Looking for user: " + username);
        return userRepository.findByUsername(username)
                .doOnNext(user -> System.out.println("[CustomUserDetailsService] Found user: " + user.getUsername() + ", password: " + user.getPassword()))
                .map(this::createUserDetails)
                .doOnNext(userDetails -> System.out.println("[CustomUserDetailsService] Created UserDetails: " + userDetails.getUsername()))
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("[CustomUserDetailsService] User not found: " + username);
                    return Mono.error(new UsernameNotFoundException("User not found: " + username));
                }));
    }

    private UserDetails createUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("USER")))
                .build();
    }
} 