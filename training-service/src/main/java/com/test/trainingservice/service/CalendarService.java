package com.test.trainingservice.service;

import com.test.trainingservice.config.AppClock;
import com.test.trainingservice.dto.CalendarEventResponse;
import com.test.trainingservice.entity.LiveSession;
import com.test.trainingservice.entity.LiveSessionStatus;
import com.test.trainingservice.entity.Training;
import com.test.trainingservice.entity.TrainingStatus;
import com.test.trainingservice.exception.InvalidCalendarRangeException;
import com.test.trainingservice.exception.InvalidCalendarStatusException;
import com.test.trainingservice.exception.TrainingAccessDeniedException;
import com.test.trainingservice.exception.TrainingNotFoundException;
import com.test.trainingservice.repository.LiveSessionRepository;
import com.test.trainingservice.repository.RecordingRepository;
import com.test.trainingservice.repository.TrainingRepository;
import com.test.trainingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only projection over LiveSession (+ Training title + Recording existence) — there is
 * deliberately no CalendarRepository and no Calendar collection. LiveSession stays the single
 * source of truth; this service only shapes it for the frontend calendar view.
 */
@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final int DEFAULT_UPCOMING_LIMIT = 5;
    private static final int MAX_UPCOMING_LIMIT = 20;

    private final LiveSessionRepository liveSessionRepository;
    private final TrainingRepository trainingRepository;
    private final RecordingRepository recordingRepository;
    private final TrainingEnrollmentService trainingEnrollmentService;
    private final AppClock appClock;

    public List<CalendarEventResponse> getEvents(LocalDateTime start, LocalDateTime end,
                                                 String trainingId, String statusParam,
                                                 AuthenticatedUser user) {

        if (start == null || end == null || !start.isBefore(end)) {
            throw new InvalidCalendarRangeException("start must be provided and be before end");
        }
        LiveSessionStatus status = parseStatus(statusParam);

        List<LiveSession> sessions;

        if (trainingId != null) {
            Training training = trainingRepository.findById(trainingId)
                    .orElseThrow(() -> new TrainingNotFoundException("Training not found: " + trainingId));
            assertViewAccess(training, user);

            sessions = liveSessionRepository.findByTrainingIdAndStartAtBetweenOrderByStartAtAsc(trainingId, start, end);

        } else if (user.isAdmin()) {
            sessions = liveSessionRepository.findByStartAtBetweenOrderByStartAtAsc(start, end);

        } else {
            List<String> allowedTrainingIds = resolveAllowedTrainingIds(user);
            if (allowedTrainingIds.isEmpty()) {
                return List.of();
            }
            sessions = liveSessionRepository.findByTrainingIdInAndStartAtBetweenOrderByStartAtAsc(allowedTrainingIds, start, end);
        }

        LocalDateTime now = appClock.now();

        // status is a computed value (see LiveSession.effectiveStatus()), not a stored field
        // for SCHEDULED/LIVE/COMPLETED, so it can't be pushed into the Mongo query itself. This
        // filters in memory, but only over the result the queries above already narrowed by
        // date range + role/trainingId scope — never over the full collection.
        if (status != null) {
            sessions = sessions.stream().filter(s -> s.effectiveStatus(now) == status).toList();
        }

        return toResponses(sessions, now, user);
    }

    public List<CalendarEventResponse> getUpcoming(Integer limit, AuthenticatedUser user) {
        int effectiveLimit = clampLimit(limit);
        LocalDateTime now = appClock.now();

        List<LiveSession> sessions;
        if (user.isAdmin()) {
            sessions = liveSessionRepository.findByStartAtAfterOrderByStartAtAsc(now, PageRequest.of(0, effectiveLimit));
        } else {
            List<String> allowedTrainingIds = resolveAllowedTrainingIds(user);
            if (allowedTrainingIds.isEmpty()) {
                return List.of();
            }
            sessions = liveSessionRepository.findByTrainingIdInAndStartAtAfterOrderByStartAtAsc(
                    allowedTrainingIds, now, PageRequest.of(0, effectiveLimit));
        }

        return toResponses(sessions, now, user);
    }

    public List<CalendarEventResponse> getToday(AuthenticatedUser user) {
        LocalDate today = LocalDate.now(appClock.zone());
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        LocalDateTime now = appClock.now();

        List<LiveSession> sessions;
        if (user.isAdmin()) {
            sessions = liveSessionRepository.findByStartAtBetweenOrderByStartAtAsc(start, end);
        } else {
            List<String> allowedTrainingIds = resolveAllowedTrainingIds(user);
            if (allowedTrainingIds.isEmpty()) {
                return List.of();
            }
            sessions = liveSessionRepository.findByTrainingIdInAndStartAtBetweenOrderByStartAtAsc(allowedTrainingIds, start, end);
        }

        return toResponses(sessions, now, user);
    }

    // --- Access control — same pattern as LiveSessionService/RecordingService ---

    private void assertViewAccess(Training training, AuthenticatedUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (user.isFormateur()) {
            if (!training.getInstructorId().equals(user.userId())) {
                throw new TrainingAccessDeniedException("You cannot view another instructor's calendar");
            }
            return;
        }
        // ETUDIANT — session existence stays visible for any published-or-beyond training,
        // exactly as before. meetingUrl (private content) is gated separately in toResponses()
        // below, based on TrainingEnrollment.
        if (!training.isPublishedOrBeyond()) {
            throw new TrainingAccessDeniedException("This training is not published");
        }
    }

    /** FORMATEUR → trainings they own. ETUDIANT → published-or-beyond trainings. Never called for ADMIN. */
    private List<String> resolveAllowedTrainingIds(AuthenticatedUser user) {
        List<Training> trainings = user.isFormateur()
                ? trainingRepository.findByInstructorId(user.userId())
                : trainingRepository.findByStatusNotIn(List.of(TrainingStatus.DRAFT, TrainingStatus.CANCELLED));

        return trainings.stream().map(Training::getId).toList();
    }

    // --- Helpers ---

    private LiveSessionStatus parseStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return null;
        }
        try {
            return LiveSessionStatus.valueOf(statusParam.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCalendarStatusException("Invalid status: " + statusParam);
        }
    }

    private int clampLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_UPCOMING_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_UPCOMING_LIMIT);
    }

    /**
     * Batches the Training lookups (for trainingTitle) and the Recording existence checks
     * (for hasRecording) instead of querying once per session — avoids N+1 database calls.
     * Also batches the content-access check (meetingUrl visibility) per distinct Training in the
     * result, instead of calling TrainingEnrollmentService once per session.
     */
    private List<CalendarEventResponse> toResponses(List<LiveSession> sessions, LocalDateTime now, AuthenticatedUser user) {
        if (sessions.isEmpty()) {
            return List.of();
        }

        Set<String> trainingIds = sessions.stream().map(LiveSession::getTrainingId).collect(Collectors.toSet());
        Map<String, Training> trainingsById = trainingRepository.findAllById(trainingIds).stream()
                .collect(Collectors.toMap(Training::getId, t -> t));

        // meetingUrl is only included for trainings where the caller has actual content access
        // (ADMIN, the owning FORMATEUR, or a student with an ACTIVE TrainingEnrollment) — see
        // TrainingEnrollmentService.hasTrainingContentAccess(). Every session reaching this
        // method has already passed assertViewAccess()/resolveAllowedTrainingIds() above, so
        // title/description/startAt/endAt/status stay visible regardless; only meetingUrl is
        // conditionally stripped per training.
        Set<String> contentAccessTrainingIds = trainingsById.values().stream()
                .filter(t -> trainingEnrollmentService.hasTrainingContentAccess(t, user))
                .map(Training::getId)
                .collect(Collectors.toSet());

        Set<String> sessionIds = sessions.stream().map(LiveSession::getId).collect(Collectors.toSet());
        Set<String> sessionIdsWithRecording = recordingRepository.findBySessionIdIn(sessionIds).stream()
                .map(RecordingRepository.SessionIdOnly::getSessionId)
                .collect(Collectors.toSet());

        return sessions.stream()
                .map(session -> {
                    Training training = trainingsById.get(session.getTrainingId());
                    boolean contentAccess = contentAccessTrainingIds.contains(session.getTrainingId());
                    return CalendarEventResponse.builder()
                            .id(session.getId())
                            .trainingId(session.getTrainingId())
                            .sessionId(session.getId())
                            .trainingTitle(training != null ? training.getTitle() : null)
                            .sessionTitle(session.getTitle())
                            .description(session.getDescription())
                            .startAt(session.getStartAt())
                            .endAt(session.getEndAt())
                            .status(session.effectiveStatus(now))
                            .meetingUrl(contentAccess ? session.getMeetingUrl() : null)
                            .hasRecording(sessionIdsWithRecording.contains(session.getId()))
                            .build();
                })
                .toList();
    }
}