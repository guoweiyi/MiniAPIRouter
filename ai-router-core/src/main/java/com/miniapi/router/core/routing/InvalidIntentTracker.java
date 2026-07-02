package com.miniapi.router.core.routing;

import com.miniapi.router.core.domain.ApiKeyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InvalidIntentTracker {

    private static final Logger log = LoggerFactory.getLogger(InvalidIntentTracker.class);
    private static final int MAX_INVALID_COUNT = 3;
    private static final long ENTRY_TTL_MS = 30 * 60 * 1000;

    private final ConcurrentHashMap<String, Entry> tracker = new ConcurrentHashMap<>();

    private static class Entry {
        int invalidCount;
        Long lastSelectedKeyId;
        String lastSelectedKeyModel;
        String lastIntent;
        int lastScore;
        long lastAccessTime;
    }

    public int getInvalidCount(String sessionKey) {
        Entry entry = tracker.get(sessionKey);
        return entry != null ? entry.invalidCount : 0;
    }

    public void incrementInvalidCount(String sessionKey) {
        Entry entry = tracker.computeIfAbsent(sessionKey, k -> new Entry());
        entry.invalidCount++;
        entry.lastAccessTime = System.currentTimeMillis();
        log.info("[InvalidTracker] session={} invalidCount={}", sessionKey, entry.invalidCount);
    }

    public void resetInvalidCount(String sessionKey) {
        Entry entry = tracker.computeIfAbsent(sessionKey, k -> new Entry());
        entry.invalidCount = 0;
        entry.lastAccessTime = System.currentTimeMillis();
    }

    public void recordSelectedKey(String sessionKey, ApiKeyConfig key, String intent, int score) {
        Entry entry = tracker.computeIfAbsent(sessionKey, k -> new Entry());
        entry.lastSelectedKeyId = key.getId();
        entry.lastSelectedKeyModel = key.getModels() != null && !key.getModels().isEmpty()
                ? key.getModels().get(0) : null;
        entry.lastIntent = intent;
        entry.lastScore = score;
        entry.lastAccessTime = System.currentTimeMillis();
        log.info("[InvalidTracker] session={} recorded key_id={} intent={} score={}",
                sessionKey, key.getId(), intent, score);
    }

    public CachedResult getCachedResult(String sessionKey) {
        Entry entry = tracker.get(sessionKey);
        if (entry == null || entry.lastSelectedKeyId == null) return null;
        return new CachedResult(entry.lastSelectedKeyId, entry.lastSelectedKeyModel,
                entry.lastIntent, entry.lastScore);
    }

    public boolean shouldUseCached(String sessionKey) {
        return getInvalidCount(sessionKey) >= MAX_INVALID_COUNT;
    }

    public void clearAll() {
        int size = tracker.size();
        tracker.clear();
        log.info("[InvalidTracker] Cleared {} entries due to config change", size);
    }

    public void evictExpired() {
        long now = System.currentTimeMillis();
        tracker.entrySet().removeIf(e -> now - e.getValue().lastAccessTime > ENTRY_TTL_MS);
    }

    public record CachedResult(Long keyId, String model, String intent, int score) {}
}
