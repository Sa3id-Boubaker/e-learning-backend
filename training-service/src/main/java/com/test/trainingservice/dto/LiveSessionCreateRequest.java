package com.test.trainingservice.dto;

import com.test.trainingservice.entity.LiveSessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * application/json.
 * trainingId is intentionally absent — it comes from the URL path
 * (POST /api/trainings/{trainingId}/sessions), never from the request body.
 */
@Data
public class LiveSessionCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Start date/time is required")
    private LocalDateTime startAt;

    @NotNull(message = "End date/time is required")
    private LocalDateTime endAt;

    @URL(message = "Meeting URL must be a valid URL")
    private String meetingUrl;

    @NotNull(message = "Status is required")
    private LiveSessionStatus status;
}