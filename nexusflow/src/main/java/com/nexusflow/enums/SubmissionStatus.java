package com.nexusflow.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubmissionStatus {
    DRAFT    ("Rascunho",  "secondary"),
    PENDING  ("Pendente",  "warning"),
    APPROVED ("Aprovado",  "success"),
    REJECTED ("Rejeitado", "danger");

    private final String label;
    private final String badgeClass;
}
