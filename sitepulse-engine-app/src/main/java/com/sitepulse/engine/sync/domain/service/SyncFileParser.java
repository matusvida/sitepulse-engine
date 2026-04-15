package com.sitepulse.engine.sync.domain.service;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyncFileParser {

    private static final Pattern DATE_FOLDER = Pattern.compile("^(\\d{4})[-_]?(\\d{2})[-_]?(\\d{2})$");
    private static final Pattern FILE_TIMESTAMP = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})[_ ](\\d{2})[_:](\\d{2})[_:](\\d{2})");

    public Optional<LocalDate> parseDateFolder(String folderName) {
        Matcher matcher = DATE_FOLDER.matcher(folderName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        ));
    }

    public OffsetDateTime parseCapturedAt(String fileName, LocalDate folderDate, ZoneId sourceZone) {
        Matcher matcher = FILE_TIMESTAMP.matcher(fileName);
        if (!matcher.find()) {
            return folderDate.atStartOfDay(sourceZone).toOffsetDateTime();
        }
        LocalDateTime localDateTime = LocalDateTime.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4)),
                Integer.parseInt(matcher.group(5)),
                Integer.parseInt(matcher.group(6)),
                0
        );
        return localDateTime.atZone(sourceZone).toOffsetDateTime();
    }

    public String contentType(String fileName) {
        return ImageFormat.fromFileName(fileName)
                .orElse(ImageFormat.JPEG)
                .getMediaType();
    }
}
