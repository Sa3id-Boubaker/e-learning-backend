package com.test.forumservice.client;

import com.test.forumservice.dto.ForumReferenceSummary;
import com.test.forumservice.exception.ExternalServiceUnavailableException;
import com.test.forumservice.exception.InvalidForumReferenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Même séparation affichage (tolérant) / validation (strict) que
 * CourseServiceClient — voir sa javadoc pour le raisonnement complet.
 */
@Component
@Slf4j
public class TrainingServiceClient {

    private final RestClient restClient;

    public TrainingServiceClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://TRAINING-SERVICE").build();
    }

    // ---- Affichage (tolérant) ----

    public ForumReferenceSummary getTraining(String trainingId, String jwtToken) {
        TrainingRaw raw = fetchOrNull("/api/trainings/{id}", trainingId, jwtToken, "training", TrainingRaw.class);
        return raw == null ? null : new ForumReferenceSummary(raw.id(), raw.title());
    }

    /**
     * Tolérant — utilisée uniquement pour résoudre le FORMATEUR responsable d'un training à des
     * fins de notification (ForumPostService.createPost()). Ne lève jamais : retourne null si
     * TRAINING-SERVICE est injoignable, auquel cas l'appelant doit simplement ne pas publier de
     * notification plutôt que faire échouer la création du post.
     */
    public String getTrainingInstructorId(String trainingId, String jwtToken) {
        TrainingRaw raw = fetchOrNull("/api/trainings/{id}", trainingId, jwtToken, "training", TrainingRaw.class);
        return raw == null ? null : raw.instructorId();
    }

    public ForumReferenceSummary getLiveSession(String liveSessionId, String jwtToken) {
        LiveSessionRef ref = fetchOrNull("/api/sessions/{id}", liveSessionId, jwtToken, "live session", LiveSessionRef.class);
        return ref == null ? null : new ForumReferenceSummary(ref.id(), ref.title());
    }

    public ForumReferenceSummary getRecording(String recordingId, String jwtToken) {
        RecordingRef ref = fetchOrNull("/api/recordings/{id}", recordingId, jwtToken, "recording", RecordingRef.class);
        return ref == null ? null : new ForumReferenceSummary(ref.id(), ref.title());
    }

    private <T> T fetchOrNull(String uriTemplate, String id, String jwtToken, String kind, Class<T> type) {
        if (id == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri(uriTemplate, id)
                    .header(HttpHeaders.COOKIE, "jwt=" + jwtToken)
                    .retrieve()
                    .body(type);
        } catch (RestClientException e) {
            log.warn("TrainingServiceClient: display enrichment failed for {} id={} ({}) — showing as null instead of failing the request",
                    kind, id, e.getMessage());
            return null;
        }
    }

    // ---- Validation de chaîne (strict, création uniquement) ----

    public ForumReferenceSummary getTrainingRef(String trainingId, String jwtToken) {
        TrainingRaw raw = fetchStrict("/api/trainings/{id}", trainingId, jwtToken, "training", TrainingRaw.class);
        return raw == null ? null : new ForumReferenceSummary(raw.id(), raw.title());
    }

    public LiveSessionRef getLiveSessionRef(String liveSessionId, String jwtToken) {
        return fetchStrict("/api/sessions/{id}", liveSessionId, jwtToken, "live session", LiveSessionRef.class);
    }

    public RecordingRef getRecordingRef(String recordingId, String jwtToken) {
        return fetchStrict("/api/recordings/{id}", recordingId, jwtToken, "recording", RecordingRef.class);
    }

    private <T> T fetchStrict(String uriTemplate, String id, String jwtToken, String kind, Class<T> type) {
        if (id == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri(uriTemplate, id)
                    .header(HttpHeaders.COOKIE, "jwt=" + jwtToken)
                    .retrieve()
                    .body(type);
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidForumReferenceException("Referenced " + kind + " does not exist: " + id);
        } catch (RestClientException e) {
            log.error("TrainingServiceClient: failed to reach TRAINING-SERVICE for {} id={} ({})", kind, id, e.getMessage());
            throw new ExternalServiceUnavailableException("TRAINING-SERVICE is currently unavailable.");
        }
    }

    private record TrainingRaw(String id, String title, String instructorId) {}

    public record LiveSessionRef(String id, String trainingId, String title) {}

    public record RecordingRef(String id, String sessionId, String title) {}
}