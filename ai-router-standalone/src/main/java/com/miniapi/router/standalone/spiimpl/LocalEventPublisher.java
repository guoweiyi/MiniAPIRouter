package com.miniapi.router.standalone.spiimpl;

import com.miniapi.router.core.spi.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalEventPublisher.class);

    @Override
    public void publishLogEvent(Object event) {
        log.debug("[Event] Log event published (synchronous): {}", event.getClass().getSimpleName());
    }

    @Override
    public void publishUsageStatsEvent(Object event) {
        log.debug("[Event] Usage stats event published (synchronous): {}", event.getClass().getSimpleName());
    }
}
