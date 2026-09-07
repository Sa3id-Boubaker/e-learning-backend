package com.test.courseservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {
    private String id;
    private String text;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean correct;
}