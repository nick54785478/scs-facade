package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 弱點事件發布外向適配器 (FindingEventPublisherAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作 FindingEventPublisherPort。
 * 職責：透過 Spring ApplicationEventPublisher 發布領域事件至系統內部事件匯流排。
 * </p>
 */
@Component
public class FindingEventPublisherAdapter implements FindingEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(FindingEventPublisherAdapter.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public FindingEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event != null) {
            log.debug("發布領域事件: {} [eventId={}, aggregateType={}, aggregateId={}, occurredAt={}]",
                    event.getClass().getSimpleName(), event.eventId(), event.aggregateType(), event.aggregateId(), event.occurredAt());
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events != null) {
            for (DomainEvent event : events) {
                publish(event);
            }
        }
    }
}
