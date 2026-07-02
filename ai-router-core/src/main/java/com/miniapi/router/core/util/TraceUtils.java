package com.miniapi.router.core.util;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class TraceUtils {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TraceUtils() {}

    public static String newTraceId() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String newRequestId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return "req-" + HexFormat.of().formatHex(bytes);
    }
}
