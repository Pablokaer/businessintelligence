package com.nexusflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "form_field_responses")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FormFieldResponse {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private FormSubmission submission;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "field_id")
    private FormField field;

    @Column(columnDefinition = "TEXT")
    private String response;
}
