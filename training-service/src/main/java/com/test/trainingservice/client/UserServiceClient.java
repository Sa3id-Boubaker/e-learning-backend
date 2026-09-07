package com.test.trainingservice.client;

import com.test.trainingservice.exception.UserServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;

    public UserServiceClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://USER-SERVICE")
                .build();
    }

    /** Strict — utilisée à l'activation, doit échouer si USER-SERVICE est injoignable. */
    public Optional<UserBasicInfoResponse> getBasicInfo(String userId, String jwtToken) {
        try {
            UserBasicInfoResponse response = restClient.get()
                    .uri("/api/users/{id}/basic-info", userId)
                    .header(HttpHeaders.COOKIE, "jwt=" + jwtToken)
                    .retrieve()
                    .body(UserBasicInfoResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientResponseException e) {
            log.error("USER-SERVICE returned an error status for userId={}: {} {}", userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new UserServiceUnavailableException("Unable to verify student via User Service");
        } catch (Exception e) {
            log.error("Failed to call USER-SERVICE for userId={}", userId, e);
            throw new UserServiceUnavailableException("Unable to verify student via User Service");
        }
    }

    /** Tolérante aux pannes — pour l'enrichissement d'affichage (list/revoke). Ne lève jamais,
     * retourne null en cas d'échec pour ne pas faire tomber toute la réponse. */
    public UserBasicInfoResponse getBasicInfoOrNull(String userId, String jwtToken) {
        try {
            return restClient.get()
                    .uri("/api/users/{id}/basic-info", userId)
                    .header(HttpHeaders.COOKIE, "jwt=" + jwtToken)
                    .retrieve()
                    .body(UserBasicInfoResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch student info for enrichment, userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}