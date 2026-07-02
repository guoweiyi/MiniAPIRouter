package com.miniapi.router.core.streaming;

public class TokenCounter {

    private TokenCounter() {}

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) cjk++;
            else if (c > 0x7F) cjk++;
            else other++;
        }
        return (int) Math.ceil(cjk / 1.5 + other / 4.0);
    }

    public static int estimateMessages(String messagesJson) {
        if (messagesJson == null) return 0;
        return estimate(messagesJson);
    }
}
