package com.miniapi.router.core.protocol;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReasoningContentCache {

    private static final long TTL_MS = 300_000L;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(String reasoningContent, long timestamp) {}

    public void store(String content, String reasoningContent) {
        if (content == null || content.isEmpty() || reasoningContent == null || reasoningContent.isEmpty()) {
            return;
        }
        cache.put(content, new CacheEntry(reasoningContent, System.currentTimeMillis()));
    }

    public String lookup(String content) {
        if (content == null || content.isEmpty()) return null;
        CacheEntry entry = cache.get(content);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > TTL_MS) {
            cache.remove(content);
            return null;
        }
        return entry.reasoningContent;
    }
}
