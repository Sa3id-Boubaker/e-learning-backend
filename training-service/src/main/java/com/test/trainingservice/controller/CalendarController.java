package com.test.trainingservice.controller;

import com.test.trainingservice.dto.CalendarEventResponse;
import com.test.trainingservice.security.AuthenticatedUser;
import com.test.trainingservice.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only projection API — GET endpoints only, no POST/PUT/DELETE on purpose. There is no
 * Calendar entity behind this: everything is derived live from LiveSession (+ Training +
 * Recording) through CalendarService.
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<List<CalendarEventResponse>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String trainingId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(calendarService.getEvents(start, end, trainingId, status, user));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<CalendarEventResponse>> getUpcoming(
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(calendarService.getUpcoming(limit, user));
    }

    @GetMapping("/today")
    public ResponseEntity<List<CalendarEventResponse>> getToday(
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(calendarService.getToday(user));
    }
}