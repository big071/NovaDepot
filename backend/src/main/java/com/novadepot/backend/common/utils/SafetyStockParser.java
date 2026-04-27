package com.novadepot.backend.common.utils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SafetyStockParser {
    private static final Pattern SAFETY_STOCK_PATTERN = Pattern.compile("安全库存\\s*[=:：]\\s*(\\d+(?:\\.\\d+)?)");

    private SafetyStockParser() {
    }

    public static BigDecimal parseOrDefault(String spec, BigDecimal defaultThreshold) {
        if (spec == null || spec.isBlank()) {
            return defaultThreshold;
        }
        Matcher matcher = SAFETY_STOCK_PATTERN.matcher(spec);
        if (!matcher.find()) {
            return defaultThreshold;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (Exception ignored) {
            return defaultThreshold;
        }
    }
}
