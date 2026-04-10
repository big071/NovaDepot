package com.novadepot.backend.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class NoGenerator {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoGenerator() {
    }

    public static String next(String prefix) {
        int suffix = ThreadLocalRandom.current().nextInt(100, 999);
        return prefix + "-" + LocalDateTime.now().format(FMT) + suffix;
    }
}
