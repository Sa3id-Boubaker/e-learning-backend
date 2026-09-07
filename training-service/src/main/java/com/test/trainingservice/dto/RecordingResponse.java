package com.test.trainingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** videoPublicId is intentionally absent — it is a Cloudinary-internal technical field. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingResponse {

    private String id;
    private String sessionId;

    private String title;
    private String description;

    private String videoUrl;

    /** Duration in seconds. */
    private Long duration;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}