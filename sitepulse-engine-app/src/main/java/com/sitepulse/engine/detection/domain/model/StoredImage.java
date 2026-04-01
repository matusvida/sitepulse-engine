package com.sitepulse.engine.detection.domain.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class StoredImage {

    private final Integer id;
    private final String bucket;
    private final String key;
    private final OffsetDateTime capturedAt;
}
