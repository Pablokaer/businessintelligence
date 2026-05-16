package com.nexusflow.entity;

import com.nexusflow.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "form_fields")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FormField {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private FormTemplate template;

    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", length = 20, nullable = false)
    @Builder.Default
    private FieldType fieldType = FieldType.TEXT;

    @Builder.Default
    private Boolean required = false;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    public String[] getOptionsArray() {
        if (options == null || options.isBlank()) return new String[0];
        return options.split(",");
    }
}
