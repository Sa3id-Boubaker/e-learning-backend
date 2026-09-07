package com.test.notificationservice.controller;

import com.test.notificationservice.dto.*;
import com.test.notificationservice.security.AuthenticatedUser;
import com.test.notificationservice.service.NotificationService;
import com.test.notificationservice.service.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping("/my")
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(notificationService.getMyNotifications(page, size, currentUser));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUser));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<MarkReadResponse> markAsRead(@PathVariable String id,
                                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser));
    }

    @PatchMapping("/my/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(notificationService.markAllAsRead(currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        notificationService.deleteNotification(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return sseEmitterRegistry.subscribe(currentUser.userId());
    }
}