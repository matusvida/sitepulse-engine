package com.sitepulse.engine.detection.domain.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ImageDetectionCompletedEvent implements DomainEvent {

    private final Integer imageId;
    private final Integer projectId;
    private final String modelVersion;
    private final int detectionCount;
}
