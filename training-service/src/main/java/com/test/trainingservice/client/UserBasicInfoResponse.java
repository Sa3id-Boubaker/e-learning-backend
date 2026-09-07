package com.test.trainingservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Verified against the real USER-SERVICE response for GET /api/users/{id}/basic-info. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}