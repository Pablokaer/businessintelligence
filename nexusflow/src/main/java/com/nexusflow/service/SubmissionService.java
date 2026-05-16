package com.nexusflow.service;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.Submission;
import com.nexusflow.entity.User;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.exception.BusinessException;
import com.nexusflow.exception.NotFoundException;
import com.nexusflow.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class SubmissionService {

    private final SubmissionRepository repo;

    public Page<Submission> list(User requester, FormDTOs.SubmissionFilter f) {
        Pageable pageable = PageRequest.of(f.getPage(), f.getSize(), Sort.by("createdAt").descending());

        if (requester.hasManagerView()) {
            // OWNER, SUBMANAGER e SENIOR veem tudo da organização
            return repo.filterByManager(requester.getRootManagerId(),
                f.getStatus(), f.getType(), f.getEmployeeId(),
                f.getFrom(), f.getTo(), pageable);
        }
        return repo.findByUserId(requester.getId(), pageable);
    }

    public Submission findById(User requester, UUID id) {
        Submission s = repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Formulário não encontrado"));

        if (!requester.hasManagerView() && !s.getUser().getId().equals(requester.getId()))
            throw new BusinessException("Acesso negado");

        return s;
    }

    public List<Submission> listPending(User requester) {
        return repo.findPendingByManager(requester.getRootManagerId());
    }

    @Transactional
    public Submission create(User requester, FormDTOs.SubmissionForm form) {
        String formNumber = generateFormNumber();

        Submission s = Submission.builder()
            .user(requester)
            .type(form.getType())
            .status(SubmissionStatus.PENDING)
            .value(form.getValue())
            .hours(form.getHours())
            .formNumber(formNumber)
            .description(form.getDescription())
            .category(form.getCategory())
            .satisfaction(form.getSatisfaction())
            .notes(form.getNotes())
            .occurrenceDate(form.getOccurrenceDate())
            .build();

        Submission saved = repo.save(s);
        log.info("Submission {} created by {}", formNumber, requester.getEmail());
        return saved;
    }

    @Transactional
    public Submission review(User reviewer, UUID id, FormDTOs.ReviewForm form) {
        if (!reviewer.canReview())
            throw new BusinessException("Você não tem permissão para revisar formulários.");

        Submission s = repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Formulário não encontrado"));

        if (s.getStatus() != SubmissionStatus.PENDING)
            throw new BusinessException("Formulário já foi processado anteriormente");

        // Garante que o formulário pertence à organização do revisor
        UUID rootId = reviewer.getRootManagerId();
        User emp = s.getUser();
        boolean inOrg = emp.getId().equals(reviewer.getId())
            || (emp.getManager() != null && emp.getManager().getId().equals(rootId));
        if (!inOrg)
            throw new BusinessException("Formulário não pertence à sua organização");

        s.setStatus(form.getStatus());
        s.setReviewedBy(reviewer);
        s.setReviewedAt(OffsetDateTime.now());
        if (form.getNotes() != null && !form.getNotes().isBlank()) s.setNotes(form.getNotes());

        Submission saved = repo.save(s);
        log.info("Submission {} set to {} by {}", id, form.getStatus(), reviewer.getEmail());
        return saved;
    }

    private synchronized String generateFormNumber() {
        long count = repo.count() + 1;
        return String.format("F-%d-%05d", LocalDate.now().getYear(), count);
    }
}
