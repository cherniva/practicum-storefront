package com.cherniva.storefront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            com.cherniva.storefront.service.CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager manager = 
            new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        
        // Test password encoding
        String testPassword = "123";
        String encodedPassword = passwordEncoder.encode(testPassword);
        boolean matches = passwordEncoder.matches(testPassword, encodedPassword);
        System.out.println("[SecurityConfig] Password test - Original: " + testPassword + ", Encoded: " + encodedPassword + ", Matches: " + matches);
        
        // Test with the actual password from database
        String dbPassword = "$2a$10$TuHQiSu41dRxXr2tQDNuDO8T4BBe6TpxSBoTIHKpHp8ac2RL5RML2";
        boolean dbMatches = passwordEncoder.matches(testPassword, dbPassword);
        System.out.println("[SecurityConfig] Database password test - Matches: " + dbMatches);
        
        return manager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager authenticationManager) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/login", "/debug/user").permitAll()
                        .anyExchange().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((webFilterExchange, authentication) -> 
                            clearAllCookiesAndRedirect(webFilterExchange)
                        )
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // todo find another solution
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .build();
    }

//    @Bean
//    public WebFilter csrfTokenWebFilter() {
//        return (exchange, chain) -> {
//            CsrfToken token = exchange.getAttribute(CsrfToken.class.getName());
//            if (token != null) {
//                exchange.getAttributes().put("_csrf", token);
//            }
//            return chain.filter(exchange);
//        };
//    }
    
    private Mono<Void> clearAllCookiesAndRedirect(WebFilterExchange webFilterExchange) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        
        // Clear all cookies by setting them with max-age=0
        exchange.getRequest().getCookies().forEach((name, cookies) -> {
            cookies.forEach(cookie -> {
                ResponseCookie clearedCookie = ResponseCookie.from(name, "")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .secure(false) // Set to true if using HTTPS
                        .sameSite("Lax")
                        .build();
                exchange.getResponse().addCookie(clearedCookie);
            });
        });
        
        // Also clear common session cookies explicitly
        clearSpecificCookie(exchange, "JSESSIONID");
        clearSpecificCookie(exchange, "SESSION");
        clearSpecificCookie(exchange, "SPRING_SECURITY_CONTEXT");
        
        // Redirect to login page
        String loginUrl = "/login?logout";
        exchange.getResponse().getHeaders().add("Location", loginUrl);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        return exchange.getResponse().setComplete();
    }
    
    private void clearSpecificCookie(ServerWebExchange exchange, String cookieName) {
        ResponseCookie clearedCookie = ResponseCookie.from(cookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false) // Set to true if using HTTPS
                .sameSite("Lax")
                .build();
        exchange.getResponse().addCookie(clearedCookie);
    }
    
    private Mono<Void> redirectToLoginWithLogoutParam(WebFilterExchange webFilterExchange) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        String loginUrl = "/login?logout";
        exchange.getResponse().getHeaders().add("Location", loginUrl);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        return exchange.getResponse().setComplete();
    }
}
