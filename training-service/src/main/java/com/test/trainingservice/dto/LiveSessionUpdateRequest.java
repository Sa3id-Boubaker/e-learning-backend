package com.test.trainingservice.dto;

import com.test.trainingservice.entity.LiveSessionStatus;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * Partial update — every field is optional, only non-null fields are applied.
 * id and trainingId are intentionally absent: a session can never be moved from one
 * Training to another through PUT.
 */
@Data
public class LiveSessionUpdateRequest {

    private String title;
    private String description;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @URL(message = "Meeting URL must be a valid URL")
    private String meetingUrl;

    private LiveSessionStatus status;
}