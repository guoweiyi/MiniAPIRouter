package com.miniapi.router.core.streaming;

import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
public class UpstreamStreamClient {

    private static final Logger log = LoggerFactory.getLogger(UpstreamStreamClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record NonStreamResult(int statusCode, String body, Map<String, String> headers) {}

    public NonStreamResult callUpstream(ApiKeyConfig key, String path, Map<String, Object> body) {
        String url = buildUrl(key.getBaseUrl(), path);
        int timeoutMs = key.getTimeoutMs() != null ? key.getTimeoutMs() : 30000;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(body), StandardCharsets.UTF_8));

        addAuthHeaders(builder, key);

        String requestModel = (String) body.get("model");
        log.info("[Upstream] >>> {} {} model={} timeout={}ms", key.getProvider(), url, requestModel, timeoutMs);
        log.debug("[Upstream] >>> request body:\n{}", JsonUtils.toJson(body));

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String respBody = response.body();
            int status = response.statusCode();
            String truncated = respBody != null && respBody.length() > 500 ? respBody.substring(0, 500) : respBody;
            log.info("[Upstream] <<< {} status={} body={}", key.getProvider(), status, truncated);
            Map<String, String> headers = new HashMap<>();
            response.headers().map().forEach((k, v) -> headers.put(k, v.isEmpty() ? "" : v.get(0)));
            return new NonStreamResult(status, respBody, headers);
        } catch (Exception e) {
            log.warn("[Upstream] <<< {} FAILED: {}", key.getProvider(), e.getMessage());
            throw new RuntimeException("Upstream call failed: " + e.getMessage(), e);
        }
    }

    public BufferedReader streamUpstream(ApiKeyConfig key, String path, Map<String, Object> body) {
        String url = buildUrl(key.getBaseUrl(), path);
        int idleTimeoutMs = key.getTimeoutMs() != null ? key.getTimeoutMs() : 30000;
        long totalTimeoutMs = Math.max((long) idleTimeoutMs * 10, 600_000L);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(totalTimeoutMs))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(body), StandardCharsets.UTF_8));

        addAuthHeaders(builder, key);

        String requestModel = (String) body.get("model");
        log.info("[Upstream] >>> {} {} model={} idle_timeout={}ms total_timeout={}ms",
                key.getProvider(), url, requestModel, idleTimeoutMs, totalTimeoutMs);
        log.debug("[Upstream] >>> request body:\n{}", JsonUtils.toJson(body));

        try {
            HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            log.info("[Upstream] <<< {} status={}", key.getProvider(), status);
            if (status != 200) {
                String errBody;
                try (var is = response.body()) {
                    errBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                String truncated = errBody != null && errBody.length() > 500 ? errBody.substring(0, 500) : errBody;
                log.warn("[Upstream] <<< {} ERROR body={}", key.getProvider(), truncated);
                throw new RuntimeException("Upstream returned " + status + ": " + errBody);
            }
            InputStream idleStream = new IdleTimeoutInputStream(response.body(), idleTimeoutMs);
            return new BufferedReader(new InputStreamReader(idleStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("[Upstream] <<< {} FAILED: {}", key.getProvider(), e.getMessage());
            throw new RuntimeException("Stream upstream failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Upstream] <<< {} INTERRUPTED", key.getProvider());
            throw new RuntimeException("Stream upstream interrupted", e);
        }
    }

    private static class IdleTimeoutInputStream extends InputStream {
        private final InputStream delegate;
        private final long idleTimeoutMs;
        private volatile long lastReadTime;
        private final Thread watcher;

        IdleTimeoutInputStream(InputStream delegate, long idleTimeoutMs) {
            this.delegate = delegate;
            this.idleTimeoutMs = idleTimeoutMs;
            this.lastReadTime = System.currentTimeMillis();
            this.watcher = Thread.ofVirtual().start(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(Math.min(idleTimeoutMs / 2, 1000));
                        if (System.currentTimeMillis() - lastReadTime > idleTimeoutMs) {
                            delegate.close();
                            return;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (IOException ignored) {
                        return;
                    }
                }
            });
        }

        @Override
        public int read() throws IOException {
            int result = delegate.read();
            if (result != -1) {
                lastReadTime = System.currentTimeMillis();
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = delegate.read(b, off, len);
            if (result != -1) {
                lastReadTime = System.currentTimeMillis();
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            watcher.interrupt();
            delegate.close();
        }
    }

    private void addAuthHeaders(HttpRequest.Builder builder, ApiKeyConfig key) {
        String protocol = key.getProtocol() != null ? key.getProtocol() : "openai";
        if ("anthropic".equalsIgnoreCase(protocol)) {
            builder.header("x-api-key", key.getApiKey());
            builder.header("anthropic-version", "2023-06-01");
        } else {
            builder.header("Authorization", "Bearer " + key.getApiKey());
        }
    }

    private static String buildUrl(String baseUrl, String defaultPath) {
        if (baseUrl == null || baseUrl.isBlank()) return defaultPath;
        String trimmed = baseUrl.replaceAll("/+$", "");

        URI uri = URI.create(trimmed);
        String urlPath = uri.getPath();
        String endpoint = defaultPath.replaceFirst("/v\\d+", "");

        if (trimmed.endsWith(endpoint)) {
            return trimmed;
        }

        if (urlPath != null && !urlPath.isEmpty() && !"/".equals(urlPath)) {
            return trimmed + endpoint;
        }

        return trimmed + defaultPath;
    }
}
