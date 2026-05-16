package com.nexusflow.dto;

import com.nexusflow.enums.AccessLevel;
import com.nexusflow.enums.FieldType;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.enums.SubmissionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FormDTOs {

    @Data
    public static class LoginForm {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class CreateEmployeeForm {
        @NotBlank @Size(min = 2, max = 120)
        private String name;
        @NotBlank @Size(min = 1, max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Use apenas letras, números, ponto, hífen ou underscore")
        private String emailPrefix;
        @NotBlank @Size(min = 4, max = 60)
        private String password;
        /** Nível de acesso inicial; padrão STANDARD. */
        private AccessLevel accessLevel = AccessLevel.STANDARD;
    }

    /** Atualizar apenas o nível de acesso de um funcionário já existente. */
    @Data
    public static class UpdateAccessForm {
        @NotNull
        private AccessLevel accessLevel;
    }

    @Data
    public static class SubmissionForm {
        @NotNull
        private SubmissionType type;
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        private BigDecimal value;
        @NotNull @DecimalMin("0.1") @DecimalMax("99.9")
        private BigDecimal hours;
        @NotBlank @Size(max = 500)
        private String description;
        @NotBlank @Size(max = 100)
        private String category;
        @Min(1) @Max(5)
        private Short satisfaction;
        @Size(max = 2000)
        private String notes;
        @NotNull
        @DateTimeFormat(iso = ISO.DATE)
        private LocalDate occurrenceDate = LocalDate.now();
    }

    @Data
    public static class ReviewForm {
        @NotNull
        private SubmissionStatus status;
        @Size(max = 500)
        private String notes;
    }

    @Data
    public static class CreateOwnerForm {
        @NotBlank @Size(min = 2, max = 120)
        private String name;
        @NotBlank @Size(min = 2, max = 120)
        private String comercio;
        private boolean useCustomSlug;
        @Size(max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Use apenas letras e números, sem acentos ou espaços")
        private String comercioSlug;
        @NotBlank @Size(min = 1, max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Use apenas letras, números, ponto, hífen ou underscore")
        private String emailPrefix;
        @NotBlank @Size(min = 4, max = 60)
        private String password;
    }

    @Data
    public static class ChangeOwnerPasswordForm {
        @NotBlank @Size(min = 4, max = 60)
        private String newPassword;
    }

    @Data
    public static class SubmissionFilter {
        private SubmissionStatus status;
        private SubmissionType type;
        private UUID employeeId;
        private LocalDate from;
        private LocalDate to;
        private int page = 0;
        private int size = 20;
    }

    // ── Form builder DTOs ─────────────────────────────────────

    @Data
    public static class CreateTemplateForm {
        @NotBlank @Size(min = 2, max = 120)
        private String name;
        @Size(max = 500)
        private String description;
    }

    @Data
    public static class AddFieldForm {
        @NotBlank @Size(min = 1, max = 120)
        private String label;
        @NotNull
        private FieldType fieldType = FieldType.TEXT;
        private boolean required = false;
        @Size(max = 1000)
        private String options;
        private int displayOrder = 0;
    }

    @Data
    public static class FieldResponseItem {
        private String fieldId;
        private String response;
    }

    @Data
    public static class FormFillForm {
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
        private BigDecimal serviceCost = BigDecimal.ZERO;
        @NotNull @DecimalMin("0") @Digits(integer = 3, fraction = 2)
        private BigDecimal serviceHours = BigDecimal.ZERO;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
        private BigDecimal serviceValue = BigDecimal.ZERO;
        @NotNull
        @DateTimeFormat(iso = ISO.DATE)
        private LocalDate occurrenceDate = LocalDate.now();
        @Size(max = 2000)
        private String notes;
        private List<FieldResponseItem> responses = new ArrayList<>();
    }

    @Data
    public static class ServiceDataForm {
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
        private BigDecimal serviceCost = BigDecimal.ZERO;
        @NotNull @DecimalMin("0") @Digits(integer = 3, fraction = 2)
        private BigDecimal serviceHours = BigDecimal.ZERO;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
        private BigDecimal serviceValue = BigDecimal.ZERO;
        @NotNull
        @DateTimeFormat(iso = ISO.DATE)
        private LocalDate occurrenceDate = LocalDate.now();
        @Size(max = 2000)
        private String notes;
    }

    @Data
    public static class GuestFillForm {
        private List<FieldResponseItem> responses = new ArrayList<>();
    }

    @Data
    public static class FormSubmissionFilter {
        private SubmissionStatus status;
        private LocalDate from;
        private LocalDate to;
        private int page = 0;
        private int size = 20;
    }

    @Data
    public static class ReviewFormSubmissionForm {
        @NotNull
        private SubmissionStatus status;
        @Size(max = 500)
        private String notes;
    }
}

