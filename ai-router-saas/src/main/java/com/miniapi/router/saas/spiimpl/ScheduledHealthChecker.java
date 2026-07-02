package com.miniapi.router.saas.spiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.spi.ApiKeyConfigRepository;
import com.miniapi.router.core.spi.HealthChecker;
import com.miniapi.router.core.util.CryptoUtils;
import com.miniapi.router.saas.entity.ApiKeyConfigDO;
import com.miniapi.router.saas.mapper.ApiKeyConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ScheduledHealthChecker implements HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(ScheduledHealthChecker.class);
    private static final int FAILURE_THRESHOLD = 3;
    private static final int PROBE_TIMEOUT_SECONDS = 10;

    private final ApiKeyConfigMapper mapper;
    private final ApiKeyConfigRepository keyRepository;
    private final CryptoUtils cryptoUtils;
    private final Map<Long, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private final HttpClient httpClient;

    public ScheduledHealthChecker(ApiKeyConfigMapper mapper, ApiKeyConfigRepository keyRepository,
                                  CryptoUtils cryptoUtils) {
        this.mapper = mapper;
        this.keyRepository = keyRepository;
        this.cryptoUtils = cryptoUtils;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(PROBE_TIMEOUT_SECONDS))
                .build();
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledCheck() {
        LambdaQueryWrapper<ApiKeyConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyConfigDO::getStatus, 1).eq(ApiKeyConfigDO::getDeleted, 0);
        List<ApiKeyConfigDO> keys = mapper.selectList(wrapper);
        log.info("[HealthCheck] Probing {} active key configs", keys.size());
        for (ApiKeyConfigDO dO : keys) {
            try {
                probe(dO);
            } catch (Exception e) {
                log.warn("[HealthCheck] Error probing key {}: {}", dO.getId(), e.getMessage());
            }
        }
    }

    private void probe(ApiKeyConfigDO dO) {
        String apiKey = cryptoUtils.decrypt(dO.getApiKeyEnc());
        String baseUrl = dO.getBaseUrl();
        String protocol = dO.getProtocol() != null ? dO.getProtocol() : "openai";
        String probePath = "anthropic".equalsIgnoreCase(protocol) ? "/v1/models" : "/v1/models";

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + probePath))
                    .timeout(Duration.ofSeconds(PROBE_TIMEOUT_SECONDS))
                    .GET();

            if ("anthropic".equalsIgnoreCase(protocol)) {
                reqBuilder.header("x-api-key", apiKey);
                reqBuilder.header("anthropic-version", "2023-06-01");
            } else {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<Void> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();

            if (status >= 200 && status < 500) {
                markHealthy(dO.getId());
            } else {
                log.warn("[HealthCheck] Key {} probe returned status {}", dO.getId(), status);
                markDown(dO.getId(), "HTTP " + status);
            }
        } catch (Exception e) {
            log.warn("[HealthCheck] Key {} probe failed: {}", dO.getId(), e.getMessage());
            markDown(dO.getId(), e.getClass().getSimpleName());
        }
    }

    @Override
    public void check(ApiKeyConfig config) {
        if (config == null) return;
        ApiKeyConfigDO dO = mapper.selectById(config.getId());
        if (dO != null) probe(dO);
    }

    @Override
    public String getStatus(Long keyId) {
        AtomicInteger count = failureCounts.get(keyId);
        if (count == null || count.get() == 0) return "healthy";
        if (count.get() >= FAILURE_THRESHOLD) return "down";
        return "degraded";
    }

    @Override
    public void markDown(Long keyId, String reason) {
        AtomicInteger count = failureCounts.computeIfAbsent(keyId, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        if (newCount >= FAILURE_THRESHOLD) {
            keyRepository.updateHealthStatus(keyId, "down");
        } else {
            keyRepository.updateHealthStatus(keyId, "degraded");
        }
    }

    @Override
    public void markHealthy(Long keyId) {
        AtomicInteger count = failureCounts.get(keyId);
        if (count != null) count.set(0);
        keyRepository.updateHealthStatus(keyId, "healthy");
    }
}
