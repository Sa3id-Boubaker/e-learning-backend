package com.test.courseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/** Anonymous-safe — no instructor/owner fields, no description. For the public landing page only. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCourseResponse {
    private String id;
    private String title;
    private String category;
    private String image;
    private BigDecimal price;
    private BigDecimal finalPrice;
}