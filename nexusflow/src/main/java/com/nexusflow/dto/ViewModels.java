package com.nexusflow.dto;

import lombok.Builder;
import lombok.Data;

import com.nexusflow.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ViewModels {

    @Data @Builder
    public static class SummaryVM {
        private BigDecimal totalSales;
        private BigDecimal totalExpenses;
        private BigDecimal totalServices;
        private BigDecimal totalRefunds;
        private BigDecimal netBalance;
        private long totalSubmissions;
        private long pendingCount;
        private long approvedCount;
        private LocalDate from;
        private LocalDate to;
    }

    @Data @Builder
    public static class CategoryVM {
        private String category;
        private BigDecimal total;
        private long count;
        private double pct;
    }

    @Data @Builder
    public static class EmployeeStatsVM {
        private UUID id;
        private String name;
        private BigDecimal total;
        private long count;
        private BigDecimal hours;
    }

    @Data @Builder
    public static class DailySaleVM {
        private LocalDate date;
        private BigDecimal value;
    }

    @Data @Builder
    public static class OwnerStatsVM {
        private User owner;
        private BigDecimal sales;
        private BigDecimal expenses;
        private BigDecimal services;
        private BigDecimal refunds;
        private BigDecimal netBalance;
        private long pendingCount;
    }
}
