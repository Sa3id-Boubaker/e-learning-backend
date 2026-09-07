package com.test.trainingservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Single source of "now" for the whole service, always resolved against the configured
 * app.timezone (Africa/Tunis) rather than the JVM/server default zone. Used everywhere a
 * status needs to be computed from the current time (LiveSession SCHEDULED/LIVE/COMPLETED,
 * Training IN_PROGRESS/COMPLETED, Calendar's today/upcoming) so all of them agree on the
 * same clock.
 */
@Component
public class AppClock {

    private final ZoneId zoneId;

    public AppClock(@Value("${app.timezone}") String timezoneId) {
        this.zoneId = ZoneId.of(timezoneId);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(zoneId);
    }

    public ZoneId zone() {
        return zoneId;
    }
}