package com.test.trainingservice.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination wrapper — the project had no established paginated-response convention
 * yet (Training/LiveSession/Calendar lists are all unpaginated), so this introduces one for
 * TrainingEnrollment's admin list and student "/my" endpoints, in the shape requested:
 * content/page/size/totalElements/totalPages.
 */
@Data
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}