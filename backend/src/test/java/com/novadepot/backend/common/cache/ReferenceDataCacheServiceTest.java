package com.novadepot.backend.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceDataCacheServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ReferenceDataCacheService service = new ReferenceDataCacheService(redisTemplate, objectMapper);

    @Test
    void getList_shouldReturnCachedValueWithoutCallingLoader() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("novadepot:ref:tenant:1:products:list"))
                .thenReturn("[{\"productCode\":\"SKU-001\"}]");
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        List<CacheProduct> rows = service.getList("novadepot:ref:tenant:1:products:list",
                new TypeReference<List<CacheProduct>>() {
                },
                () -> {
                    loaderCalled.set(true);
                    return List.of(new CacheProduct("DB"));
                });

        assertThat(rows).extracting(CacheProduct::productCode).containsExactly("SKU-001");
        assertThat(loaderCalled).isFalse();
        verify(valueOperations, never()).set(any(), any(), any());
    }

    @Test
    void getList_shouldLoadAndWriteCacheWhenMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("novadepot:ref:tenant:1:warehouses:list")).thenReturn(null);

        List<CacheProduct> rows = service.getList("novadepot:ref:tenant:1:warehouses:list",
                new TypeReference<List<CacheProduct>>() {
                },
                () -> List.of(new CacheProduct("WH-01")));

        assertThat(rows).extracting(CacheProduct::productCode).containsExactly("WH-01");
        verify(valueOperations).set(eq("novadepot:ref:tenant:1:warehouses:list"), any(String.class), any());
    }

    @Test
    void getList_shouldFallbackToLoaderWhenRedisReadFails() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        List<CacheProduct> rows = service.getList("novadepot:ref:tenant:1:locations:list",
                new TypeReference<List<CacheProduct>>() {
                },
                () -> List.of(new CacheProduct("LOC-01")));

        assertThat(rows).extracting(CacheProduct::productCode).containsExactly("LOC-01");
    }

    @Test
    void evictPrefix_shouldDeleteMatchingKeysOnly() {
        when(redisTemplate.keys("novadepot:ref:tenant:1:partners:*"))
                .thenReturn(Set.of("novadepot:ref:tenant:1:partners:list"));

        service.evictPrefix("novadepot:ref:tenant:1:partners:");

        verify(redisTemplate).delete(Set.of("novadepot:ref:tenant:1:partners:list"));
    }

    private record CacheProduct(String productCode) {
    }
}
