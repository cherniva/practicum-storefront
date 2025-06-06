package com.cherniva.storefront.config;

import com.cherniva.storefront.model.Product;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Настройка сериализации ключей (строки)
        template.setKeySerializer(new StringRedisSerializer());

        // Настройка сериализации значений (JSON)
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

        return template;
    }

//    @Bean
//    public RedisCacheManagerBuilderCustomizer weatherCacheCustomizer() {
//        return builder -> builder.withCacheConfiguration(
//                "products",                                         // Имя кеша
//                RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(Duration.of(1, ChronoUnit.MINUTES))  // TTL
//                        .serializeValuesWith(                          // Сериализация JSON
//                                RedisSerializationContext.SerializationPair.fromSerializer(
//                                        new Jackson2JsonRedisSerializer<>(Product.class)
//                                )
//                        )
//        );
//    }

//    @Bean
//    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
//        // Key serializer
//        StringRedisSerializer keySerializer = new StringRedisSerializer();
//
//        // Value serializer using Jackson
//        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
//
//        // Build serialization context
//        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
//                RedisSerializationContext.newSerializationContext(keySerializer);
//
//        RedisSerializationContext<String, Object> context = builder
//                .value(valueSerializer)
//                .build();
//
//        return new ReactiveRedisTemplate<>(connectionFactory, context);
//    }

}
