package com.sitepulse.engine.common.application.event;

public interface DomainEventPublisher {

    void publish(Object event);
}
