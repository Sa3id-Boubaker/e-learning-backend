package com.test.trainingservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;

/**
 * A LiveSession belongs to exactly one Training, referenced by trainingId only —
 * no embedded Training object, no DB-level relationship. Ownership is always resolved
 * by looking up the parent Training and checking Training.instructorId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "live_sessions")
@CompoundIndex(name = "training_start_idx", def = "{'trainingId': 1, 'startAt': 1}")
public class LiveSession {

    @Id
    private String id;

    private String trainingId;

    private String title;
    private String description;

    @Indexed
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String meetingUrl;

    private LiveSessionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * SCHEDULED/LIVE/COMPLETED are fully derivable from startAt/endAt vs. now — no instructor
     * should have to remember to click a button when a session starts or ends. CANCELLED is the
     * one state that can never be inferred (it's a human decision), so a stored CANCELLED always
     * wins over whatever the clock says. The stored `status` field itself is left untouched by
     * this method — nothing here writes back to the database.
     */
    public LiveSessionStatus effectiveStatus(LocalDateTime now) {
        if (status == LiveSessionStatus.CANCELLED) {
            return LiveSessionStatus.CANCELLED;
        }
        if (now.isBefore(startAt)) {
            return LiveSessionStatus.SCHEDULED;
        }
        if (now.isAfter(endAt)) {
            return LiveSessionStatus.COMPLETED;
        }
        return LiveSessionStatus.LIVE;
    }

}