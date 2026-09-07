package com.test.courseservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://USER-SERVICE")
                .build();
    }

    public UserBasicInfo fetchBasicInfo(String userId, String jwtToken) {
        try {
            return restClient.get()
                    .uri("/api/users/{id}/basic-info", userId)
                    .header(HttpHeaders.COOKIE, "jwt=" + jwtToken)
                    .retrieve()
                    .body(UserBasicInfo.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch instructor info for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    public record UserBasicInfo(String id, String firstName, String lastName, String email, String profileImage, String role) {}
}