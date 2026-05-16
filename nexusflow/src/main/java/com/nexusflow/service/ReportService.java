package com.nexusflow.service;

import com.nexusflow.dto.ViewModels;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.enums.SubmissionType;
import com.nexusflow.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class ReportService {

    private final SubmissionRepository repo;

    @Transactional(readOnly = true)
    public ViewModels.SummaryVM summary(UUID managerId, LocalDate from, LocalDate to) {
        BigDecimal sales    = orZero(repo.sumByManagerAndType(managerId, SubmissionType.SALE,    from, to));
        BigDecimal expenses = orZero(repo.sumByManagerAndType(managerId, SubmissionType.EXPENSE, from, to));
        BigDecimal services = orZero(repo.sumByManagerAndType(managerId, SubmissionType.SERVICE, from, to));
        BigDecimal refunds  = orZero(repo.sumByManagerAndType(managerId, SubmissionType.REFUND,  from, to));
        long total   = repo.filterByManager(managerId, null, null, null, from, to, Pageable.unpaged()).getTotalElements();
        long pending = repo.countPendingByManager(managerId);

        return ViewModels.SummaryVM.builder()
            .totalSales(sales).totalExpenses(expenses)
            .totalServices(services).totalRefunds(refunds)
            .netBalance(sales.add(services).subtract(expenses).subtract(refunds))
            .totalSubmissions(total).pendingCount(pending)
            .from(from).to(to)
            .build();
    }

    @Transactional(readOnly = true)
    public List<ViewModels.CategoryVM> byCategory(UUID managerId, LocalDate from, LocalDate to) {
        var rows = repo.groupByCategory(managerId, from, to);
        BigDecimal grand = rows.stream().map(r -> toBigDecimal(r[1])).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream().map(r -> {
            BigDecimal val = toBigDecimal(r[1]);
            double pct = grand.compareTo(BigDecimal.ZERO) == 0 ? 0
                : val.multiply(BigDecimal.valueOf(100)).divide(grand, 1, RoundingMode.HALF_UP).doubleValue();
            return ViewModels.CategoryVM.builder()
                .category(r[0] != null ? r[0].toString() : "")
                .total(val).count(((Number) r[2]).longValue()).pct(pct).build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ViewModels.EmployeeStatsVM> byEmployee(UUID managerId, LocalDate from, LocalDate to) {
        return repo.groupByEmployee(managerId, from, to).stream().map(r ->
            ViewModels.EmployeeStatsVM.builder()
                .id((UUID) r[0]).name((String) r[1])
                .total(toBigDecimal(r[2])).count(((Number) r[3]).longValue())
                .hours(toBigDecimal(r[4])).build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<ViewModels.DailySaleVM> dailySales(UUID managerId, LocalDate from, LocalDate to) {
        return repo.dailySales(managerId, from, to).stream().map(r ->
            ViewModels.DailySaleVM.builder().date((LocalDate) r[0]).value(toBigDecimal(r[1])).build()
        ).toList();
    }

    // Employee summary
    @Transactional(readOnly = true)
    public ViewModels.SummaryVM summaryForEmployee(UUID userId, LocalDate from, LocalDate to) {
        BigDecimal sales    = orZero(repo.sumByUserAndType(userId, SubmissionType.SALE,    from, to));
        BigDecimal expenses = orZero(repo.sumByUserAndType(userId, SubmissionType.EXPENSE, from, to));
        BigDecimal services = orZero(repo.sumByUserAndType(userId, SubmissionType.SERVICE, from, to));
        BigDecimal refunds  = orZero(repo.sumByUserAndType(userId, SubmissionType.REFUND,  from, to));
        long pending  = repo.countByUserIdAndStatus(userId, SubmissionStatus.PENDING);
        long approved = repo.countByUserIdAndStatus(userId, SubmissionStatus.APPROVED);
        return ViewModels.SummaryVM.builder()
            .totalSales(sales).totalExpenses(expenses)
            .totalServices(services).totalRefunds(refunds)
            .netBalance(sales.add(services).subtract(expenses).subtract(refunds))
            .pendingCount(pending).approvedCount(approved)
            .from(from).to(to).build();
    }

    private BigDecimal orZero(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(v.toString());
    }
}
