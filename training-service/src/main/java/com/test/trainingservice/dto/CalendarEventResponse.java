package com.test.trainingservice.dto;

import com.test.trainingservice.entity.LiveSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A pure read projection over LiveSession (+ Training title + Recording existence) — there is
 * no Calendar entity/collection behind this. id and sessionId are intentionally identical
 * (session.getId() for both): "id" is there so the Angular frontend can map it straight to
 * FullCalendar's event.id without any renaming, while "sessionId" keeps the field explicit for
 * everything else that reads this API.
 *
 * meetingUrl is not annotated with @JsonInclude(NON_NULL) — it doesn't need to be. Every event
 * in a CalendarService response has already passed the same view-access check LiveSessionService
 * uses, so there is no code path where an unauthorized viewer receives an event containing a
 * meetingUrl at all (same pattern as LiveSessionResponse/RecordingResponse).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventResponse {

    private String id;
    private String trainingId;
    private String sessionId;

    private String trainingTitle;
    private String sessionTitle;
    private String description;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private LiveSessionStatus status;
    private String meetingUrl;

    private boolean hasRecording;
}