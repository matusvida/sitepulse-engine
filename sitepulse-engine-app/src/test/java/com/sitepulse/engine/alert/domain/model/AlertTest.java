package com.sitepulse.engine.alert.domain.model;

import com.sitepulse.engine.alert.domain.enums.AlertSeverity;
import com.sitepulse.engine.alert.domain.enums.AlertStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    private Alert createOpenAlert() {
        return Alert.create(1, "stall", AlertSeverity.HIGH, "summary", "details", List.of("action1"), NOW);
    }

    @Test
    void createReturnsOpenAlert() {
        Alert alert = createOpenAlert();
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertNull(alert.getUpdatedAt());
    }

    @Test
    void acknowledgeFromOpenSucceeds() {
        Alert alert = createOpenAlert();
        alert.acknowledge(NOW.plusMinutes(1));
        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
        assertNotNull(alert.getUpdatedAt());
    }

    @Test
    void acknowledgeFromAcknowledgedFails() {
        Alert alert = createOpenAlert();
        alert.acknowledge(NOW.plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> alert.acknowledge(NOW.plusMinutes(2)));
    }

    @Test
    void resolveFromOpenSucceeds() {
        Alert alert = createOpenAlert();
        alert.resolve(NOW.plusMinutes(1));
        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
    }

    @Test
    void resolveFromAcknowledgedSucceeds() {
        Alert alert = createOpenAlert();
        alert.acknowledge(NOW.plusMinutes(1));
        alert.resolve(NOW.plusMinutes(2));
        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
    }

    @Test
    void resolveAlreadyResolvedFails() {
        Alert alert = createOpenAlert();
        alert.resolve(NOW.plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> alert.resolve(NOW.plusMinutes(2)));
    }

    @Test
    void updateStatusAcknowledgedDelegatesToAcknowledge() {
        Alert alert = createOpenAlert();
        alert.updateStatus(AlertStatus.ACKNOWLEDGED, NOW.plusMinutes(1));
        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
    }

    @Test
    void updateStatusResolvedDelegatesToResolve() {
        Alert alert = createOpenAlert();
        alert.updateStatus(AlertStatus.RESOLVED, NOW.plusMinutes(1));
        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
    }

    @Test
    void updateStatusOpenThrows() {
        Alert alert = createOpenAlert();
        assertThrows(IllegalArgumentException.class, () -> alert.updateStatus(AlertStatus.OPEN, NOW));
    }
}
