package com.nexusflow.service;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.*;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.exception.BusinessException;
import com.nexusflow.exception.NotFoundException;
import com.nexusflow.repository.FormSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormSubmissionService {

    private final FormSubmissionRepository repo;
    private final FormTemplateService templateService;

    @Transactional
    public FormSubmission submit(User employee, UUID templateId, FormDTOs.FormFillForm form) {
        FormTemplate template = templateService.findById(templateId);
        if (!template.getActive()) throw new BusinessException("Este formulário não está disponível.");

        FormSubmission submission = FormSubmission.builder()
            .template(template)
            .submittedBy(employee)
            .serviceCost(form.getServiceCost())
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
                template.getFields().stream()
                    .filter(f -> f.getId().equals(fieldId))
                    .findFirst()
                    .ifPresent(field -> {
                        if (field.getRequired() && (item.getResponse() == null || item.getResponse().isBlank()))
                            throw new BusinessException("Campo obrigatório: " + field.getLabel());
                        responses.add(FormFieldResponse.builder()
                            .submission(submission)
                            .field(field)
                            .response(item.getResponse())
                            .build());
                    });
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
            .serviceCost(form.getServiceCost())
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
    public void submitGuestResponses(UUID token, FormDTOs.GuestFillForm form) {
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
                template.getFields().stream()
                    .filter(f -> f.getId().equals(fieldId))
                    .findFirst()
                    .ifPresent(field -> {
                        if (field.getRequired() && (item.getResponse() == null || item.getResponse().isBlank()))
                            throw new BusinessException("Campo obrigatório: " + field.getLabel());
                        responses.add(FormFieldResponse.builder()
                            .submission(draft)
                            .field(field)
                            .response(item.getResponse())
                            .build());
                    });
            }
        }
        draft.getResponses().clear();
        draft.getResponses().addAll(responses);
        // Mantém DRAFT — funcionário confirma e submete manualmente
        repo.save(draft);
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
