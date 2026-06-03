package com.novadepot.backend.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class ReferenceDataCacheService {
    private static final Logger log = LoggerFactory.getLogger(ReferenceDataCacheService.class);
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ReferenceDataCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> List<T> getList(String key, TypeReference<List<T>> type, Supplier<List<T>> loader) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception e) {
            log.warn("Reference cache read skipped for key {}", key);
        }
        List<T> loaded = loader.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(loaded), TTL);
        } catch (Exception e) {
            log.warn("Reference cache write skipped for key {}", key);
        }
        return loaded;
    }

    public void evictPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Reference cache evict skipped for prefix {}", prefix);
        }
    }
}

