package com.miniapi.router.core.spi;

public interface EventPublisher {
    void publishLogEvent(Object event);
    void publishUsageStatsEvent(Object event);
}
