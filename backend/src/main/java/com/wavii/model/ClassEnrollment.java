package com.wavii.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_enrollments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(length = 300)
    private String teacherName;

    @Column(length = 300)
    private String studentName;

    @Column(length = 120)
    private String instrument;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String province;

    @Column(length = 20)
    private String modality;

    @Column(length = 20)
    private String requestedModality;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "request_message", length = 1000)
    private String requestMessage;

    @Column(name = "request_availability", length = 500)
    private String requestAvailability;

    @Column(nullable = false)
    @Builder.Default
    private Integer hoursPurchased = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer hoursUsed = 0;

    @Column(length = 120)
    private String stripePaymentIntentId;

    @Column(length = 300)
    private String paymentReceiptNumber;

    @Column(length = 300)
    private String classLink;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
