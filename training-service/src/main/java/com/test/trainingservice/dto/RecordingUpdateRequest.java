package com.test.trainingservice.dto;

import lombok.Data;

/**
 * Metadata-only partial update. videoUrl, videoPublicId, sessionId and duration are
 * intentionally absent — none of them can ever be changed through this endpoint.
 * The video itself is replaced only through POST /api/recordings/{id}/video.
 */
@Data
public class RecordingUpdateRequest {

    private String title;
    private String description;
}