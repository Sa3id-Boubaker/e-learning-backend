package com.test.notificationservice.service;

import com.test.notificationservice.dto.*;
import com.test.notificationservice.exception.NotificationAccessDeniedException;
import com.test.notificationservice.exception.ResourceNotFoundException;
import com.test.notificationservice.model.Notification;
import com.test.notificationservice.repository.NotificationRepository;
import com.test.notificationservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public PageResponse<NotificationResponse> getMyNotifications(int page, int size, AuthenticatedUser currentUser) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = notificationRepository.findByUserId(currentUser.userId(), pageable);

        List<NotificationResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<NotificationResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    public UnreadCountResponse getUnreadCount(AuthenticatedUser currentUser) {
        long count = notificationRepository.countByUserIdAndReadFalse(currentUser.userId());
        return new UnreadCountResponse(count);
    }

    public MarkReadResponse markAsRead(String id, AuthenticatedUser currentUser) {
        Notification notification = resolveOwnedNotification(id, currentUser);

        notification.setRead(true);
        notificationRepository.save(notification);

        return new MarkReadResponse(notification.getId(), true);
    }

    public MarkAllReadResponse markAllAsRead(AuthenticatedUser currentUser) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(currentUser.userId());

        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);

        return new MarkAllReadResponse(unread.size());
    }

    public void deleteNotification(String id, AuthenticatedUser currentUser) {
        Notification notification = resolveOwnedNotification(id, currentUser);
        notificationRepository.delete(notification);
    }

    private Notification resolveOwnedNotification(String id, AuthenticatedUser currentUser) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(currentUser.userId())) {
            throw new NotificationAccessDeniedException("You do not have access to this notification");
        }

        return notification;
    }

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}