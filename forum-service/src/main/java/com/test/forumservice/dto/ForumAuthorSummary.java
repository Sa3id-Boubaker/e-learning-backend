package com.test.forumservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumAuthorSummary {

    private String id;
    private String firstName;
    private String lastName;
    private String profileImage;
    private String role;
}