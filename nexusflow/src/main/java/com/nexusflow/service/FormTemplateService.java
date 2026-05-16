package com.nexusflow.service;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.FormField;
import com.nexusflow.entity.FormSubmission;
import com.nexusflow.entity.FormTemplate;
import com.nexusflow.entity.User;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.exception.BusinessException;
import com.nexusflow.exception.NotFoundException;
import com.nexusflow.repository.FormSubmissionRepository;
import com.nexusflow.repository.FormTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormTemplateService {

    private final FormTemplateRepository templateRepo;
    private final FormSubmissionRepository submissionRepo;

    @Transactional(readOnly = true)
    public List<FormTemplate> listByManager(User manager) {
        return templateRepo.findByOwnerIdOrderByCreatedAtDesc(resolveOwnerId(manager));
    }

    @Transactional(readOnly = true)
    public List<FormTemplate> listActiveForEmployee(User employee) {
        UUID rootId = employee.getRootManagerId();
        if (rootId == null) return List.of();
        return templateRepo.findByOwnerIdAndActiveTrueOrderByName(rootId);
    }

    @Transactional
    public FormTemplate create(User manager, FormDTOs.CreateTemplateForm form) {
        FormTemplate t = FormTemplate.builder()
            .owner(resolveOwner(manager))
            .name(form.getName().trim())
            .description(form.getDescription())
            .build();
        return templateRepo.save(t);
    }

    @Transactional
    public FormTemplate addField(UUID templateId, FormDTOs.AddFieldForm form) {
        FormTemplate t = findById(templateId);
        int nextOrder = t.getFields().isEmpty() ? 0 : t.getFields().size();
        FormField field = FormField.builder()
            .template(t)
            .label(form.getLabel().trim())
            .fieldType(form.getFieldType())
            .required(form.isRequired())
            .options(form.getOptions())
            .displayOrder(form.getDisplayOrder() > 0 ? form.getDisplayOrder() : nextOrder)
            .build();
        t.getFields().add(field);
        return templateRepo.save(t);
    }

    @Transactional
    public void removeField(UUID templateId, UUID fieldId) {
        FormTemplate t = findById(templateId);
        t.getFields().removeIf(f -> f.getId().equals(fieldId));
        templateRepo.save(t);
    }

    @Transactional
    public void delete(UUID templateId) {
        FormTemplate t = findById(templateId);
        submissionRepo.deleteByTemplateId(templateId);
        templateRepo.delete(t);
    }

    @Transactional
    public FormTemplate toggleActive(UUID templateId) {
        FormTemplate t = findById(templateId);
        t.setActive(!t.getActive());
        return templateRepo.save(t);
    }

    @Transactional(readOnly = true)
    public FormTemplate findById(UUID id) {
        return templateRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Template não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<FormSubmission> listSubmissions(UUID templateId, FormDTOs.FormSubmissionFilter filter) {
        return submissionRepo.filterByTemplate(
            templateId,
            filter.getStatus(),
            filter.getFrom(),
            filter.getTo(),
            PageRequest.of(filter.getPage(), filter.getSize()));
    }

    @Transactional(readOnly = true)
    public Page<FormSubmission> listAllSubmissions(User manager, FormDTOs.FormSubmissionFilter filter) {
        return submissionRepo.filterByOwner(
            resolveOwnerId(manager),
            filter.getStatus(),
            filter.getFrom(),
            filter.getTo(),
            PageRequest.of(filter.getPage(), filter.getSize()));
    }

    private UUID resolveOwnerId(User manager) {
        if (manager.isOwner()) return manager.getId();
        return manager.getManager() != null ? manager.getManager().getId() : manager.getId();
    }

    private User resolveOwner(User manager) {
        if (manager.isOwner()) return manager;
        return manager.getManager() != null ? manager.getManager() : manager;
    }
}
