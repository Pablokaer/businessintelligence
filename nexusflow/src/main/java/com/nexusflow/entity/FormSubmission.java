package com.nexusflow.entity;

import com.nexusflow.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "form_submissions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FormSubmission {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private FormTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "submission_status")
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "service_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal serviceCost = BigDecimal.ZERO;

    @Column(name = "service_hours", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal serviceHours = BigDecimal.ZERO;

    @Column(name = "service_value", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal serviceValue = BigDecimal.ZERO;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "share_token", unique = true)
    private UUID shareToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FormFieldResponse> responses = new ArrayList<>();

    @CreatedDate @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
