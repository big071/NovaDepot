package com.novadepot.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtTokenService {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final Set<String> REJECTED_PLACEHOLDERS = Set.of(
            "change_me_in_prod",
            "change_me_in_prod_please",
            "change_me_in_dev_please",
            "change_me_in_dev_please_change",
            "change_me_in_test_please"
    );
    private final JwtProperties jwtProperties;
    private final Environment environment;

    public JwtTokenService(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @PostConstruct
    void validateOnStartup() {
        validateSecret();
    }

    public String createToken(Long userId, Long tenantId, String username, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "uid", userId,
                        "tid", tenantId,
                        "perms", permissions
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getExpireSeconds())))
                .signWith(secretKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        validateSecret();
        byte[] key = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(key);
    }

    private void validateSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        String trimmed = secret.trim();
        if (trimmed.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        if (REJECTED_PLACEHOLDERS.contains(trimmed)) {
            throw new IllegalStateException("JWT_SECRET uses an unsafe placeholder");
        }
        boolean productionLike = Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
        if (productionLike && trimmed.toLowerCase().contains("local_dev")) {
            throw new IllegalStateException("Production JWT_SECRET must not use a local development placeholder");
        }
    }
}
