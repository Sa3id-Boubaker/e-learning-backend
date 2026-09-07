package com.test.courseservice.event;

import com.test.courseservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEnrollmentActivated(String studentId, String courseId, String courseTitle,
                                           BigDecimal amount, LocalDateTime activatedAt) {

        EnrollmentActivatedEvent event = new EnrollmentActivatedEvent(
                UUID.randomUUID().toString(), studentId, courseId, courseTitle, amount, activatedAt);

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENROLLMENT_ACTIVATED, event);
        } catch (AmqpException e) {
            // L'inscription elle-même a DÉJÀ réussi (MongoDB déjà sauvegardé) — une panne RabbitMQ
            // ne doit jamais faire échouer l'activation aux yeux de l'admin. Voir section
            // "Limitations connues" : c'est exactement le trou que comblerait l'Outbox Pattern.
            log.error("Failed to publish EnrollmentActivatedEvent for studentId={}, courseId={}: {}",
                    studentId, courseId, e.getMessage());
        }
    }
}