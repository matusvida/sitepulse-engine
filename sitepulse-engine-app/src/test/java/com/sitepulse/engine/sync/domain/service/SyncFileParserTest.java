package com.sitepulse.engine.sync.domain.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyncFileParserTest {

    private final SyncFileParser parser = new SyncFileParser();

    @Test
    void parseDateFolderWithDashes() {
        Optional<LocalDate> date = parser.parseDateFolder("2024-06-15");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2024, 6, 15), date.get());
    }

    @Test
    void parseDateFolderWithUnderscores() {
        Optional<LocalDate> date = parser.parseDateFolder("2024_06_15");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2024, 6, 15), date.get());
    }

    @Test
    void parseDateFolderCompact() {
        Optional<LocalDate> date = parser.parseDateFolder("20240615");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2024, 6, 15), date.get());
    }

    @Test
    void parseDateFolderInvalidReturnsEmpty() {
        assertTrue(parser.parseDateFolder("random-folder").isEmpty());
    }

    @Test
    void parseCapturedAtFromFilename() {
        OffsetDateTime result = parser.parseCapturedAt("cam1_2024-06-15_10_30_00.jpg", LocalDate.of(2024, 6, 15));
        assertEquals(OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC), result);
    }

    @Test
    void parseCapturedAtFallsBackToFolderDate() {
        OffsetDateTime result = parser.parseCapturedAt("image.jpg", LocalDate.of(2024, 6, 15));
        assertEquals(LocalDate.of(2024, 6, 15).atStartOfDay().atOffset(ZoneOffset.UTC), result);
    }

    @Test
    void contentTypeForPng() {
        assertEquals("image/png", parser.contentType("photo.PNG"));
    }

    @Test
    void contentTypeDefaultsToJpeg() {
        assertEquals("image/jpeg", parser.contentType("photo.jpg"));
    }
}
