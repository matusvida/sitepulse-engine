package com.sitepulse.engine.common.application.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
