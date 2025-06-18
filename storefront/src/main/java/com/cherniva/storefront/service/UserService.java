package com.cherniva.storefront.service;

import com.cherniva.storefront.model.User;
import com.cherniva.storefront.repository.UserR2dbcRepository;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Service
public class UserService {
    private final UserR2dbcRepository userRepository;

    public UserService(UserR2dbcRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> getActiveUserMono() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)
                .flatMap(userRepository::findByUsername);
    }

    public Mono<Long> getActiveUserIdMono() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName)
                .flatMap(userRepository::findByUsername)
                .map(User::getId);
    }
}
