package com.nexusflow.service;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.dto.ViewModels;
import com.nexusflow.entity.User;
import com.nexusflow.enums.AccessLevel;
import com.nexusflow.enums.Role;
import com.nexusflow.enums.SubmissionType;
import com.nexusflow.exception.BusinessException;
import com.nexusflow.repository.SubmissionRepository;
import com.nexusflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> listOwners() {
        return userRepository.findByRoleAndAccessLevelOrderByName(Role.MANAGER, AccessLevel.OWNER);
    }

    @Transactional
    public User createOwner(FormDTOs.CreateOwnerForm form) {
        String domain = (form.isUseCustomSlug()
                && form.getComercioSlug() != null
                && !form.getComercioSlug().isBlank())
            ? form.getComercioSlug().toLowerCase().trim()
            : slugify(form.getComercio());
        String email  = form.getEmailPrefix().toLowerCase().trim() + "@" + domain + ".com";

        if (userRepository.existsByEmail(email))
            throw new BusinessException("E-mail já cadastrado: " + email);

        User owner = User.builder()
            .name(form.getName())
            .comercio(form.getComercio())
            .email(email)
            .passwordHash(passwordEncoder.encode(form.getPassword()))
            .role(Role.MANAGER)
            .accessLevel(AccessLevel.OWNER)
            .active(true)
            .build();

        User saved = userRepository.save(owner);
        log.info("Owner created by SUPER_ADMIN: {}", saved.getEmail());
        return saved;
    }

    public static String slugify(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized
            .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "");
    }

    @Transactional
    public void toggleOwnerAccess(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
            .filter(u -> u.getAccessLevel() == AccessLevel.OWNER)
            .orElseThrow(() -> new BusinessException("Owner não encontrado"));
        owner.setActive(!owner.getActive());
        userRepository.save(owner);
        log.info("Owner {} access set to active={}", owner.getEmail(), owner.getActive());
    }

    @Transactional
    public void changeOwnerPassword(UUID ownerId, FormDTOs.ChangeOwnerPasswordForm form) {
        User owner = userRepository.findById(ownerId)
            .filter(u -> u.getAccessLevel() == AccessLevel.OWNER)
            .orElseThrow(() -> new BusinessException("Owner não encontrado"));
        owner.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(owner);
        log.info("Password changed for owner {}", owner.getEmail());
    }

    @Transactional(readOnly = true)
    public List<ViewModels.OwnerStatsVM> statsPerOwner(List<UUID> ownerIds, LocalDate from, LocalDate to) {
        return ownerIds.stream()
            .map(id -> userRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .map(owner -> buildOwnerStats(owner, from, to))
            .toList();
    }

    @Transactional(readOnly = true)
    public ViewModels.SummaryVM compiledSummary(List<UUID> ownerIds, LocalDate from, LocalDate to) {
        if (ownerIds.isEmpty()) return emptyCompiled(from, to);
        BigDecimal sales    = orZero(submissionRepository.sumByManagersAndType(ownerIds, SubmissionType.SALE,    from, to));
        BigDecimal expenses = orZero(submissionRepository.sumByManagersAndType(ownerIds, SubmissionType.EXPENSE, from, to));
        BigDecimal services = orZero(submissionRepository.sumByManagersAndType(ownerIds, SubmissionType.SERVICE, from, to));
        BigDecimal refunds  = orZero(submissionRepository.sumByManagersAndType(ownerIds, SubmissionType.REFUND,  from, to));
        long pending = submissionRepository.countPendingByManagers(ownerIds);
        return ViewModels.SummaryVM.builder()
            .totalSales(sales).totalExpenses(expenses)
            .totalServices(services).totalRefunds(refunds)
            .netBalance(sales.add(services).subtract(expenses).subtract(refunds))
            .pendingCount(pending).from(from).to(to).build();
    }

    private ViewModels.OwnerStatsVM buildOwnerStats(User owner, LocalDate from, LocalDate to) {
        UUID id = owner.getId();
        BigDecimal sales    = orZero(submissionRepository.sumByManagerAndType(id, SubmissionType.SALE,    from, to));
        BigDecimal expenses = orZero(submissionRepository.sumByManagerAndType(id, SubmissionType.EXPENSE, from, to));
        BigDecimal services = orZero(submissionRepository.sumByManagerAndType(id, SubmissionType.SERVICE, from, to));
        BigDecimal refunds  = orZero(submissionRepository.sumByManagerAndType(id, SubmissionType.REFUND,  from, to));
        long pending = submissionRepository.countPendingByManager(id);
        return ViewModels.OwnerStatsVM.builder()
            .owner(owner).sales(sales).expenses(expenses)
            .services(services).refunds(refunds)
            .netBalance(sales.add(services).subtract(expenses).subtract(refunds))
            .pendingCount(pending).build();
    }

    private ViewModels.SummaryVM emptyCompiled(LocalDate from, LocalDate to) {
        return ViewModels.SummaryVM.builder()
            .totalSales(BigDecimal.ZERO).totalExpenses(BigDecimal.ZERO)
            .totalServices(BigDecimal.ZERO).totalRefunds(BigDecimal.ZERO)
            .netBalance(BigDecimal.ZERO).pendingCount(0).from(from).to(to).build();
    }

    private BigDecimal orZero(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
