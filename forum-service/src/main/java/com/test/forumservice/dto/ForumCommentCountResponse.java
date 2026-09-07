package com.test.forumservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForumCommentCountResponse {
    private long count;
}