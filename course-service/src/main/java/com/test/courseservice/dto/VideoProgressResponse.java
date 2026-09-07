package com.test.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoProgressResponse {
    private String videoId;
    private boolean completed;
}