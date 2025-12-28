package com.example.vacancyscraper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Ensures Redis cache can serialize JPA entities/DTOs (LocalDate, etc.) via Jackson JSON.
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(name = "redisCacheConfiguration")
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));

        CacheProperties.Redis redisProps = cacheProperties.getRedis();
        if (redisProps.getTimeToLive() != null) {
            config = config.entryTtl(redisProps.getTimeToLive());
        }
        if (!redisProps.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProps.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        if (redisProps.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProps.getKeyPrefix());
        }
        return config;
    }
}
