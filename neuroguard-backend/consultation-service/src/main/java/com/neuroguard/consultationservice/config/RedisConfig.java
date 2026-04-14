package com.neuroguard.consultationservice.config;

import com.neuroguard.consultationservice.dto.LocationUpdateDto;
import com.neuroguard.consultationservice.dto.DistanceMatrixResultDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Configuration Redis pour caching et streaming de données.
 * Utilise la sérialisation JSON générique de Spring Boot.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.redis.host", matchIfMissing = false)
public class RedisConfig {

    /**
     * Crée un RedisTemplate pour LocationUpdateDto.
     * Retourne un bean de type RedisTemplate<String, LocationUpdateDto> pour LocationStreamingService.
     */
    @Bean
    public RedisTemplate<String, LocationUpdateDto> locationUpdateRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, LocationUpdateDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Crée un RedisTemplate pour DistanceMatrixResultDto.
     * Retourne un bean de type RedisTemplate<String, DistanceMatrixResultDto> pour GoogleRoadDistanceService.
     */
    @Bean
    public RedisTemplate<String, DistanceMatrixResultDto> distanceMatrixRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, DistanceMatrixResultDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Crée un RedisTemplate générique pour tous les types d'objets.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}


