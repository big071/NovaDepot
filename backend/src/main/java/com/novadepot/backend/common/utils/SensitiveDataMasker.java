package com.novadepot.backend.common.utils;

import java.util.List;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final List<Pattern> KEY_VALUE_PATTERNS = List.of(
            Pattern.compile("(?i)(api[-_ ]?key\\s*[:=]\\s*)([^\\s,;\\\"']{8,})"),
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+)([^\\s,;\\\"']{8,})"),
            Pattern.compile("(?i)(jwt[-_ ]?secret\\s*[:=]\\s*)([^\\s,;\\\"']{8,})"),
            Pattern.compile("(?i)(password\\s*[:=]\\s*)([^\\s,;\\\"']{4,})"),
            Pattern.compile("(?i)(token\\s*[:=]\\s*)([^\\s,;\\\"']{8,})")
    );
    private static final Pattern DEEPSEEK_KEY = Pattern.compile("sk-[A-Za-z0-9_-]{8,}");
    private static final Pattern JWT_LIKE = Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");

    private SensitiveDataMasker() {
    }

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = value;
        for (Pattern pattern : KEY_VALUE_PATTERNS) {
            masked = pattern.matcher(masked).replaceAll(match -> match.group(1) + maskToken(match.group(2)));
        }
        masked = DEEPSEEK_KEY.matcher(masked).replaceAll(match -> maskToken(match.group()));
        masked = JWT_LIKE.matcher(masked).replaceAll(match -> maskToken(match.group()));
        return masked;
    }

    public static String maskToken(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
