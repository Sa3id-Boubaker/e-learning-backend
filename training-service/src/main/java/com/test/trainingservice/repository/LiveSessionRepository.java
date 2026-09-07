package com.test.trainingservice.repository;

import com.test.trainingservice.entity.LiveSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LiveSessionRepository extends MongoRepository<LiveSession, String> {

    /** Sorted at the database level — GET /api/trainings/{trainingId}/sessions relies on this. */
    List<LiveSession> findByTrainingIdOrderByStartAtAsc(String trainingId);

    /** Unsorted — used internally for overlap detection within the same training. */
    List<LiveSession> findByTrainingId(String trainingId);

    // --- Calendar — all database-level filtering, no findAll()+Java filtering anywhere below.
    // Note: there is no status-parameterized query here anymore. SCHEDULED/LIVE/COMPLETED are
    // now computed from startAt/endAt vs. now (see LiveSession.effectiveStatus()), not stored
    // reliably, so a status filter can't be expressed as a static field match — CalendarService
    // applies it in memory, on the already date-range- and role-scoped result these queries
    // return (never on a full unbounded collection).

    /** Used by TrainingService.getDeletionImpact() to report the count before a cascade delete. */
    long countByTrainingId(String trainingId);

    /** Used by TrainingService.delete() to cascade-delete every session of a training. */
    void deleteByTrainingId(String trainingId);

    /** ADMIN, no trainingId filter. */
    List<LiveSession> findByStartAtBetweenOrderByStartAtAsc(LocalDateTime start, LocalDateTime end);

    /** FORMATEUR/ETUDIANT, scoped to their authorized training IDs. */
    List<LiveSession> findByTrainingIdInAndStartAtBetweenOrderByStartAtAsc(Collection<String> trainingIds, LocalDateTime start, LocalDateTime end);

    /** Explicit trainingId filter (?trainingId=...). */
    List<LiveSession> findByTrainingIdAndStartAtBetweenOrderByStartAtAsc(String trainingId, LocalDateTime start, LocalDateTime end);

    /** /api/calendar/upcoming for ADMIN — Pageable carries the limit. */
    List<LiveSession> findByStartAtAfterOrderByStartAtAsc(LocalDateTime start, Pageable pageable);

    /** /api/calendar/upcoming for FORMATEUR/ETUDIANT, scoped to their authorized training IDs. */
    List<LiveSession> findByTrainingIdInAndStartAtAfterOrderByStartAtAsc(Collection<String> trainingIds, LocalDateTime start, Pageable pageable);
}