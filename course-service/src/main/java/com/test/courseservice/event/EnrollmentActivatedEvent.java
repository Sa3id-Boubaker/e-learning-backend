package com.test.courseservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentActivatedEvent(
        String eventId,
        String studentId,
        String courseId,
        String courseTitle,
        BigDecimal amount,
        LocalDateTime activatedAt
) {}