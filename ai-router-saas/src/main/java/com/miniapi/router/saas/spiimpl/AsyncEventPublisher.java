package com.miniapi.router.saas.spiimpl;

import com.miniapi.router.core.spi.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AsyncEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    public AsyncEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishLogEvent(Object event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publishUsageStatsEvent(Object event) {
        publisher.publishEvent(event);
    }
}
