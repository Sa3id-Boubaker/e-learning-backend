package com.test.trainingservice.dto;

import com.test.trainingservice.entity.LiveSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * meetingUrl IS included here on purpose — but this response is only ever built after
 * LiveSessionService has already passed assertViewAccess() for the caller, so an
 * unauthorized user never reaches the point where a response containing meetingUrl is
 * constructed. See LiveSessionService for the access checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionResponse {

    private String id;
    private String trainingId;

    private String title;
    private String description;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String meetingUrl;

    private LiveSessionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}