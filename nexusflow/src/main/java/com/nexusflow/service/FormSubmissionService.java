package com.nexusflow.service;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.*;
import com.nexusflow.enums.FieldType;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.exception.BusinessException;
import com.nexusflow.exception.NotFoundException;
import com.nexusflow.repository.FormSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FormSubmissionService {

    private final FormSubmissionRepository repo;
    private final FormTemplateService templateService;
    private final FileStorageService fileStorageService;

    @Transactional
    public FormSubmission submit(User employee, UUID templateId, FormDTOs.FormFillForm form,
                                 Map<String, MultipartFile> fileUploads) {
        FormTemplate template = templateService.findById(templateId);
        if (!template.getActive()) throw new BusinessException("Este formulário não está disponível.");

        FormSubmission submission = FormSubmission.builder()
            .template(template)
            .submittedBy(employee)
            .serviceCost(BigDecimal.ZERO)
            .serviceHours(form.getServiceHours())
            .serviceValue(form.getServiceValue())
            .occurrenceDate(form.getOccurrenceDate())
            .notes(form.getNotes())
            .build();

        List<FormFieldResponse> responses = new ArrayList<>();
        if (form.getResponses() != null) {
            for (FormDTOs.FieldResponseItem item : form.getResponses()) {
                if (item.getFieldId() == null || item.getFieldId().isBlank()) continue;
                UUID fieldId = UUID.fromString(item.getFieldId());
                Optional<FormField> fieldOpt = template.getFields().stream()
                    .filter(f -> f.getId().equals(fieldId)).findFirst();
                if (fieldOpt.isEmpty()) continue;
                FormField field = fieldOpt.get();

                String responseValue = resolveResponse(field, fieldId, item.getResponse(), fileUploads);
                if (responseValue == null) continue;
                responses.add(FormFieldResponse.builder()
                    .submission(submission).field(field).response(responseValue).build());
            }
        }
        submission.setResponses(responses);
        return repo.save(submission);
    }

    @Transactional(readOnly = true)
    public FormSubmission findById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Registro não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<FormSubmission> listByEmployee(User employee, int page, int size) {
        return repo.findBySubmittedByIdOrderByCreatedAtDesc(employee.getId(), PageRequest.of(page, size));
    }

    @Transactional
    public FormSubmission createDraft(User employee, UUID templateId, FormDTOs.ServiceDataForm form) {
        FormTemplate template = templateService.findById(templateId);
        if (!template.getActive()) throw new BusinessException("Este formulário não está disponível.");

        FormSubmission draft = FormSubmission.builder()
            .template(template)
            .submittedBy(employee)
            .status(SubmissionStatus.DRAFT)
            .shareToken(UUID.randomUUID())
            .serviceCost(BigDecimal.ZERO)
            .serviceHours(form.getServiceHours())
            .serviceValue(form.getServiceValue())
            .occurrenceDate(form.getOccurrenceDate())
            .notes(form.getNotes())
            .build();

        return repo.save(draft);
    }

    @Transactional(readOnly = true)
    public FormSubmission findDraftByToken(UUID token) {
        FormSubmission draft = repo.findByShareToken(token)
            .filter(s -> s.getStatus() == SubmissionStatus.DRAFT)
            .orElseThrow(() -> new com.nexusflow.exception.NotFoundException("Link inválido ou não disponível."));
        if (!draft.getResponses().isEmpty())
            throw new com.nexusflow.exception.NotFoundException("Este formulário já foi preenchido.");
        return draft;
    }

    @Transactional
    public void submitGuestResponses(UUID token, FormDTOs.GuestFillForm form,
                                     Map<String, MultipartFile> fileUploads) {
        FormSubmission draft = repo.findByShareToken(token)
            .filter(s -> s.getStatus() == SubmissionStatus.DRAFT)
            .orElseThrow(() -> new com.nexusflow.exception.NotFoundException("Link inválido ou não disponível."));
        if (!draft.getResponses().isEmpty())
            throw new BusinessException("Este formulário já foi preenchido.");

        FormTemplate template = draft.getTemplate();
        List<FormFieldResponse> responses = new ArrayList<>();

        if (form.getResponses() != null) {
            for (FormDTOs.FieldResponseItem item : form.getResponses()) {
                if (item.getFieldId() == null || item.getFieldId().isBlank()) continue;
                UUID fieldId = UUID.fromString(item.getFieldId());
                Optional<FormField> fieldOpt = template.getFields().stream()
                    .filter(f -> f.getId().equals(fieldId)).findFirst();
                if (fieldOpt.isEmpty()) continue;
                FormField field = fieldOpt.get();

                String responseValue = resolveResponse(field, fieldId, item.getResponse(), fileUploads);
                if (responseValue == null) continue;
                responses.add(FormFieldResponse.builder()
                    .submission(draft).field(field).response(responseValue).build());
            }
        }
        draft.getResponses().clear();
        draft.getResponses().addAll(responses);
        repo.save(draft);
    }

    private String resolveResponse(FormField field, UUID fieldId, String textResponse,
                                   Map<String, MultipartFile> fileUploads) {
        if (field.getFieldType() == FieldType.FILE) {
            MultipartFile file = fileUploads != null ? fileUploads.get("file_" + fieldId) : null;
            if (file == null || file.isEmpty()) {
                if (field.getRequired())
                    throw new BusinessException("Campo obrigatório: " + field.getLabel());
                return null;
            }
            try {
                return fileStorageService.store(file);
            } catch (IOException e) {
                throw new BusinessException("Erro ao salvar arquivo: " + field.getLabel());
            }
        }
        if (field.getRequired() && (textResponse == null || textResponse.isBlank()))
            throw new BusinessException("Campo obrigatório: " + field.getLabel());
        return textResponse;
    }

    @Transactional(readOnly = true)
    public boolean isDraftFilledByGuest(UUID draftId) {
        FormSubmission draft = repo.findById(draftId)
            .orElseThrow(() -> new NotFoundException("Rascunho não encontrado"));
        return !draft.getResponses().isEmpty();
    }

    @Transactional
    public void deletePending(User employee, UUID submissionId) {
        FormSubmission s = repo.findById(submissionId)
            .orElseThrow(() -> new NotFoundException("Registro não encontrado"));
        if (!s.getSubmittedBy().getId().equals(employee.getId()))
            throw new BusinessException("Você não tem permissão para excluir este registro.");
        if (s.getStatus() != SubmissionStatus.DRAFT)
            throw new BusinessException("Apenas rascunhos podem ser excluídos.");
        repo.delete(s);
    }

    @Transactional
    public void submitDraft(UUID draftId) {
        FormSubmission draft = repo.findById(draftId)
            .filter(s -> s.getStatus() == SubmissionStatus.DRAFT)
            .orElseThrow(() -> new BusinessException("Rascunho não encontrado ou já submetido."));
        draft.setStatus(SubmissionStatus.PENDING);
        repo.save(draft);
    }

    @Transactional
    public FormSubmission review(User manager, UUID id, FormDTOs.ReviewFormSubmissionForm form) {
        FormSubmission s = findById(id);
        if (s.getStatus() != SubmissionStatus.PENDING)
            throw new BusinessException("Registro já foi revisado.");
        s.setStatus(form.getStatus());
        s.setReviewedBy(manager);
        s.setReviewedAt(OffsetDateTime.now());
        if (form.getNotes() != null && !form.getNotes().isBlank())
            s.setNotes(s.getNotes() != null ? s.getNotes() + "\n[Manager]: " + form.getNotes() : form.getNotes());
        return repo.save(s);
    }
}
