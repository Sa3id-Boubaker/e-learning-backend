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
 * Deux familles de méthodes, volontairement séparées :
 *
 *  - Affichage (getCourse/getChapter/getVideo) : appelées à la LECTURE
 *    (GET /posts, GET /posts/{id}) pour enrichir la réponse. TOLÉRANTES —
 *    un échec (service injoignable, référence supprimée depuis) renvoie
 *    simplement null, jamais une exception. Sans ça, un seul post dont la
 *    référence est momentanément injoignable ferait tomber TOUTE la liste
 *    en 503, y compris des posts qui n'ont rien à voir avec cette
 *    référence-là.
 *
 *  - Validation (getChapterRef/getVideoRef) : appelées uniquement à la
 *    CRÉATION d'un post, pour vérifier qu'une référence existe vraiment et
 *    re-dériver son id parent réel. STRICTES — un échec doit bloquer la
 *    création (400 si la référence n'existe pas, 503 si le service est
 *    injoignable), sinon un post pourrait être créé avec une référence
 *    invalide.
 */
@Component
@Slf4j
public class CourseServiceClient {

    private final RestClient restClient;

    public CourseServiceClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://COURSE-SERVICE").build();
    }

    // ---- Affichage (tolérant) ----

    public ForumReferenceSummary getCourse(String courseId, String jwtToken) {
        CourseRaw raw = fetchOrNull("/api/courses/{id}", courseId, jwtToken, "course", CourseRaw.class);
        return raw == null ? null : new ForumReferenceSummary(raw.id(), raw.title());
    }

    public ForumReferenceSummary getChapter(String chapterId, String jwtToken) {
        ChapterRef ref = fetchOrNull("/api/chapters/{id}", chapterId, jwtToken, "chapter", ChapterRef.class);
        return ref == null ? null : new ForumReferenceSummary(ref.id(), ref.title());
    }

    public ForumReferenceSummary getVideo(String videoId, String jwtToken) {
        VideoRef ref = fetchOrNull("/api/videos/{id}", videoId, jwtToken, "video", VideoRef.class);
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
            log.warn("CourseServiceClient: display enrichment failed for {} id={} ({}) — showing as null instead of failing the request",
                    kind, id, e.getMessage());
            return null;
        }
    }

    // ---- Validation de chaîne (strict, création uniquement) ----

    public ChapterRef getChapterRef(String chapterId, String jwtToken) {
        return fetchStrict("/api/chapters/{id}", chapterId, jwtToken, "chapter", ChapterRef.class);
    }

    public VideoRef getVideoRef(String videoId, String jwtToken) {
        return fetchStrict("/api/videos/{id}", videoId, jwtToken, "video", VideoRef.class);
    }

    public ForumReferenceSummary getCourseStrict(String courseId, String jwtToken) {
        CourseRaw raw = fetchStrict("/api/courses/{id}", courseId, jwtToken, "course", CourseRaw.class);
        return raw == null ? null : new ForumReferenceSummary(raw.id(), raw.title());
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
            log.error("CourseServiceClient: failed to reach COURSE-SERVICE for {} id={} ({})", kind, id, e.getMessage());
            throw new ExternalServiceUnavailableException("COURSE-SERVICE is currently unavailable.");
        }
    }

    private record CourseRaw(String id, String title) {}

    public record ChapterRef(String id, String courseId, String title) {}

    public record VideoRef(String id, String chapterId, String title) {}
}