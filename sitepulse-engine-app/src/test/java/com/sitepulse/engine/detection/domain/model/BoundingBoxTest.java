package com.sitepulse.engine.detection.domain.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundingBoxTest {

    @Test
    void validBoundingBox() {
        BoundingBox bbox = new BoundingBox(List.of(10.0, 20.0, 100.0, 200.0));
        assertEquals(10.0, bbox.x1());
        assertEquals(20.0, bbox.y1());
        assertEquals(100.0, bbox.x2());
        assertEquals(200.0, bbox.y2());
    }

    @Test
    void rejectsNullCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(null));
    }

    @Test
    void rejectsWrongNumberOfCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(List.of(1.0, 2.0)));
    }

    @Test
    void immutableCopy() {
        var original = new java.util.ArrayList<>(List.of(1.0, 2.0, 3.0, 4.0));
        BoundingBox bbox = new BoundingBox(original);
        original.set(0, 999.0);
        assertEquals(1.0, bbox.x1());
    }
}
