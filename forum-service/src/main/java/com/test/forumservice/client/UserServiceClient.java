package com.test.forumservice.client;

import com.test.forumservice.dto.ForumAuthorSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Enrichissement d'affichage uniquement (nom/prénom/avatar de l'auteur).
 * Un échec ici n'est jamais bloquant : on retourne null et le mapper
 * affichera un auteur "inconnu" plutôt que de faire échouer la requête.
 * Même convention de tolérance que COURSE-SERVICE's UserServiceClient.
 */
@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://USER-SERVICE").build();
    }

    public ForumAuthorSummary getBasicInfo(String userId, String jwtToken) {
        if (userId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/users/{id}/basic-info", userId)
                    .header(HttpHeaders.COOKIE, "jwt=" + jwtToken)
                    .retrieve()
                    .body(ForumAuthorSummary.class);
        } catch (RestClientException e) {
            log.warn("UserServiceClient: failed to fetch basic info for userId={} ({})", userId, e.getMessage());
            return null;
        }
    }
}