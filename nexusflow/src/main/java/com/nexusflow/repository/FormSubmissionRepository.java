package com.nexusflow.repository;

import com.nexusflow.entity.FormSubmission;
import com.nexusflow.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {

    @Query("""
        SELECT fs FROM FormSubmission fs
        WHERE fs.template.id = :templateId
          AND fs.status       = COALESCE(:status, fs.status)
          AND fs.occurrenceDate >= COALESCE(:from, fs.occurrenceDate)
          AND fs.occurrenceDate <= COALESCE(:to,   fs.occurrenceDate)
        ORDER BY fs.createdAt DESC
    """)
    Page<FormSubmission> filterByTemplate(
        @Param("templateId") UUID templateId,
        @Param("status")     SubmissionStatus status,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to,
        Pageable pageable);

    @Query("""
        SELECT fs FROM FormSubmission fs
        WHERE fs.template.owner.id = :ownerId
          AND fs.status             = COALESCE(:status, fs.status)
          AND fs.occurrenceDate >= COALESCE(:from, fs.occurrenceDate)
          AND fs.occurrenceDate <= COALESCE(:to,   fs.occurrenceDate)
        ORDER BY fs.createdAt DESC
    """)
    Page<FormSubmission> filterByOwner(
        @Param("ownerId") UUID ownerId,
        @Param("status")  SubmissionStatus status,
        @Param("from")    LocalDate from,
        @Param("to")      LocalDate to,
        Pageable pageable);

    Page<FormSubmission> findBySubmittedByIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    java.util.Optional<FormSubmission> findByShareToken(UUID shareToken);

    @Query("""
        SELECT COALESCE(SUM(fs.serviceValue),0) FROM FormSubmission fs
        WHERE fs.template.owner.id = :ownerId AND fs.status = 'APPROVED'
          AND fs.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumServiceValueByOwner(@Param("ownerId") UUID ownerId,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(fs.serviceHours),0) FROM FormSubmission fs
        WHERE fs.template.owner.id = :ownerId AND fs.status = 'APPROVED'
          AND fs.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumServiceHoursByOwner(@Param("ownerId") UUID ownerId,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(fs.serviceCost),0) FROM FormSubmission fs
        WHERE fs.template.owner.id = :ownerId AND fs.status = 'APPROVED'
          AND fs.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumServiceCostByOwner(@Param("ownerId") UUID ownerId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(fs) FROM FormSubmission fs WHERE fs.template.owner.id = :ownerId AND fs.status = 'PENDING'")
    long countPendingByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
        SELECT COALESCE(SUM(fs.serviceValue),0) FROM FormSubmission fs
        WHERE fs.submittedBy.id = :userId AND fs.status = 'APPROVED'
          AND fs.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumServiceValueByUser(@Param("userId") UUID userId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(fs) FROM FormSubmission fs WHERE fs.submittedBy.id = :userId AND fs.status = :status")
    long countByUserAndStatus(@Param("userId") UUID userId, @Param("status") SubmissionStatus status);

    @Query("""
        SELECT COUNT(fs) FROM FormSubmission fs
        WHERE fs.template.owner.id = :ownerId
          AND fs.occurrenceDate BETWEEN :from AND :to
    """)
    long countByOwnerAndDateRange(@Param("ownerId") UUID ownerId,
                                  @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(fs.serviceHours),0) FROM FormSubmission fs
        WHERE fs.submittedBy.id = :userId AND fs.status = 'APPROVED'
          AND fs.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumServiceHoursByUser(@Param("userId") UUID userId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    void deleteByTemplateId(UUID templateId);
}
