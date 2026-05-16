package com.nexusflow.enums;

public enum FieldType {
    TEXT("Resposta curta"),
    TEXTAREA("Parágrafo"),
    NUMBER("Número"),
    DATE("Data"),
    SELECT("Lista suspensa"),
    RADIO("Múltipla escolha"),
    CHECKBOX("Caixas de seleção");

    private final String label;

    FieldType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
