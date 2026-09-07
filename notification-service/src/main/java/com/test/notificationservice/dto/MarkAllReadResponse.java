package com.test.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarkAllReadResponse {
    private long updatedCount;
}