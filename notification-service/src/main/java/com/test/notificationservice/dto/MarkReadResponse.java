package com.test.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarkReadResponse {
    private String id;
    private boolean read;
}