package com.nexusflow.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubmissionType {
    SALE    ("Venda",      "💰", "success"),
    EXPENSE ("Despesa",    "📤", "danger"),
    SERVICE ("Serviço",    "🔧", "primary"),
    REFUND  ("Reembolso",  "↩️", "warning");

    private final String label;
    private final String icon;
    private final String badgeClass;
}
