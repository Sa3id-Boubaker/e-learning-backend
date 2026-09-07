package com.test.courseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    private String id;

    private String chapterId;
    private String title;
    private String description;
    private String videoUrl;
    private String videoPublicId;
    private Integer duration;
    private Integer order;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}