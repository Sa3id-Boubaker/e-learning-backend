package com.test.trainingservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents whether a student currently has access to a Training. Payment itself is handled
 * manually outside the platform (admin verifies a bank transfer/cash payment, then activates
 * this record) — there is deliberately no Payment entity and no PENDING/WAITING_PAYMENT status.
 * A student has at most one TrainingEnrollment per Training (see the unique compound index
 * below) — revocation and reactivation reuse the same document rather than creating new ones,
 * which preserves enrolledAt as the original enrollment date across any later revoke/reactivate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "training_enrollments")
@CompoundIndexes({
        @CompoundIndex(name = "student_training_unique", def = "{'studentId': 1, 'trainingId': 1}", unique = true),
        @CompoundIndex(name = "student_status_idx", def = "{'studentId': 1, 'status': 1}"),
        @CompoundIndex(name = "training_status_idx", def = "{'trainingId': 1, 'status': 1}")
})
public class TrainingEnrollment {

    @Id
    private String id;

    private String trainingId;
    private String studentId;

    /**
     * The Training's final price (price minus discountPercentage) at the moment of activation.
     * Historical — never recalculated later, even if the Training's price changes afterward.
     */
    private BigDecimal amountAtEnrollment;

    private TrainingEnrollmentStatus status;

    /** Set once, at first creation — never touched again, even across revoke/reactivate cycles. */
    private LocalDateTime enrolledAt;

    private LocalDateTime activatedAt;
    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}