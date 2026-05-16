package com.nexusflow.repository;

import com.nexusflow.entity.Submission;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.enums.SubmissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    // ── Para employee ─────────────────────────────────────────
    Page<Submission> findByUserId(UUID userId, Pageable pageable);

    // ── Para manager (com filtros) ────────────────────────────
    // COALESCE faz PostgreSQL inferir o tipo do parâmetro a partir da coluna,
    // evitando "could not determine data type of parameter $N" com valores nulos.
    @Query("""
        SELECT s FROM Submission s
        WHERE s.user.manager.id = :mid
          AND s.status          = COALESCE(:status, s.status)
          AND s.type            = COALESCE(:type,   s.type)
          AND s.user.id         = COALESCE(:uid,    s.user.id)
          AND s.occurrenceDate >= COALESCE(:from,   s.occurrenceDate)
          AND s.occurrenceDate <= COALESCE(:to,     s.occurrenceDate)
        ORDER BY s.createdAt DESC
    """)
    Page<Submission> filterByManager(
        @Param("mid")    UUID managerId,
        @Param("status") SubmissionStatus status,
        @Param("type")   SubmissionType type,
        @Param("uid")    UUID userId,
        @Param("from")   LocalDate from,
        @Param("to")     LocalDate to,
        Pageable pageable
    );

    // ── Pendentes ─────────────────────────────────────────────
    @Query("SELECT s FROM Submission s WHERE s.user.manager.id = :mid AND s.status = 'PENDING' ORDER BY s.createdAt")
    List<Submission> findPendingByManager(@Param("mid") UUID managerId);

    long countByUserIdAndStatus(UUID userId, SubmissionStatus status);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.manager.id = :mid AND s.status = 'PENDING'")
    long countPendingByManager(@Param("mid") UUID managerId);

    // ── Relatórios ────────────────────────────────────────────
    @Query("""
        SELECT COALESCE(SUM(s.value),0) FROM Submission s
        WHERE s.user.manager.id = :mid AND s.type = :type
          AND s.status = 'APPROVED' AND s.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumByManagerAndType(@Param("mid") UUID mid, @Param("type") SubmissionType type,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(s.value),0) FROM Submission s
        WHERE s.user.id = :uid AND s.type = :type
          AND s.status = 'APPROVED' AND s.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumByUserAndType(@Param("uid") UUID uid, @Param("type") SubmissionType type,
                                @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT s.category, SUM(s.value), COUNT(s) FROM Submission s
        WHERE s.user.manager.id = :mid AND s.status = 'APPROVED'
          AND s.occurrenceDate BETWEEN :from AND :to
        GROUP BY s.category ORDER BY SUM(s.value) DESC
    """)
    List<Object[]> groupByCategory(@Param("mid") UUID mid, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT s.user.id, s.user.name, SUM(s.value), COUNT(s), SUM(s.hours) FROM Submission s
        WHERE s.user.manager.id = :mid AND s.status = 'APPROVED'
          AND s.occurrenceDate BETWEEN :from AND :to
        GROUP BY s.user.id, s.user.name ORDER BY SUM(s.value) DESC
    """)
    List<Object[]> groupByEmployee(@Param("mid") UUID mid, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        SELECT s.occurrenceDate, SUM(s.value) FROM Submission s
        WHERE s.user.manager.id = :mid AND s.type = 'SALE' AND s.status = 'APPROVED'
          AND s.occurrenceDate BETWEEN :from AND :to
        GROUP BY s.occurrenceDate ORDER BY s.occurrenceDate
    """)
    List<Object[]> dailySales(@Param("mid") UUID mid, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // ── SUPER_ADMIN — queries multi-owner ─────────────────────
    @Query("""
        SELECT COALESCE(SUM(s.value),0) FROM Submission s
        WHERE s.user.manager.id IN :mids AND s.type = :type
          AND s.status = 'APPROVED' AND s.occurrenceDate BETWEEN :from AND :to
    """)
    BigDecimal sumByManagersAndType(@Param("mids") List<UUID> mids, @Param("type") SubmissionType type,
                                    @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.manager.id IN :mids AND s.status = 'PENDING'")
    long countPendingByManagers(@Param("mids") List<UUID> mids);
}
