package com.novadepot.backend.modules.health;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public HealthController(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean mysql = mysqlUp();
        boolean redis = redisUp();
        boolean up = mysql && redis;
        return ResponseEntity.status(up ? 200 : 503)
                .body(Map.of(
                        "status", up ? "UP" : "DOWN",
                        "mysql", mysql ? "UP" : "DOWN",
                        "redis", redis ? "UP" : "DOWN"
                ));
    }

    private boolean mysqlUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean redisUp() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }
}
