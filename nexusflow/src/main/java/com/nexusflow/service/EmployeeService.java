package com.nexusflow.service;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.User;
import com.nexusflow.enums.AccessLevel;
import com.nexusflow.enums.Role;
import com.nexusflow.exception.BusinessException;
import com.nexusflow.exception.NotFoundException;
import com.nexusflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class EmployeeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Lista funcionários visíveis (ativos e bloqueados) para o usuário autenticado. */
    public List<User> listVisible(User actor) {
        UUID rootId = actor.getRootManagerId();
        return userRepository.findAllByManager(rootId);
    }

    /**
     * Cria um novo funcionário.
     * Regras:
     * - Apenas quem tem canManageEmployees() pode criar.
     * - O nível escolhido deve ser atribuível pelo actor (canAssign).
     * - SUBMANAGER cria funcionários ligados ao OWNER do comércio.
     */
    @Transactional
    public User create(User actor, FormDTOs.CreateEmployeeForm form) {
        assertCanManageEmployees(actor);

        AccessLevel level = form.getAccessLevel() != null ? form.getAccessLevel() : AccessLevel.STANDARD;
        if (!actor.getAccessLevel().canAssign(level)) {
            throw new BusinessException(
                "Você não tem permissão para criar um funcionário com nível \"" + level.getLabel() + "\".");
        }

        // O manager direto é sempre o OWNER do comércio
        UUID ownerId = actor.getRootManagerId();
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new NotFoundException("Gerente não encontrado"));

        String ownerDomain = owner.getEmail().substring(owner.getEmail().indexOf('@') + 1);
        String email = form.getEmailPrefix().toLowerCase().trim() + "@" + ownerDomain;

        if (userRepository.existsByEmail(email))
            throw new BusinessException("E-mail já cadastrado: " + email);

        User emp = User.builder()
            .name(form.getName())
            .email(email)
            .passwordHash(passwordEncoder.encode(form.getPassword()))
            .role(Role.EMPLOYEE)
            .accessLevel(level)
            .manager(owner)
            .active(true)
            .build();

        User saved = userRepository.save(emp);
        log.info("User created: {} [{}] by {} [{}]",
            saved.getEmail(), level, actor.getEmail(), actor.getAccessLevel());
        return saved;
    }

    /**
     * Altera o nível de acesso de um funcionário.
     * Regras:
     * - O actor precisa de canManageAccessLevels().
     * - actor.canManage(target.accessLevel) deve ser true.
     * - O novo nível deve ser atribuível pelo actor.
     * - Não é possível promover alguém a OWNER.
     */
    @Transactional
    public User updateAccessLevel(User actor, UUID targetId, FormDTOs.UpdateAccessForm form) {
        User target = findInOrg(actor, targetId);

        if (!actor.getAccessLevel().canManage(target.getAccessLevel())) {
            throw new BusinessException(
                "Você não tem permissão para alterar o nível de \"" + target.getName() + "\".");
        }
        if (!actor.getAccessLevel().canAssign(form.getAccessLevel())) {
            throw new BusinessException(
                "Você não pode atribuir o nível \"" + form.getAccessLevel().getLabel() + "\".");
        }

        AccessLevel old = target.getAccessLevel();
        target.setAccessLevel(form.getAccessLevel());
        User saved = userRepository.save(target);
        log.info("Access level changed: {} {} → {} by {}",
            target.getEmail(), old, form.getAccessLevel(), actor.getEmail());
        return saved;
    }

    /**
     * Desativa permanentemente (revoga acesso de) um funcionário.
     * - actor.canManage(target.accessLevel) deve ser true.
     * - Não pode desativar a si mesmo.
     */
    @Transactional
    public void deactivate(User actor, UUID targetId) {
        User target = findInOrgAny(actor, targetId);

        if (target.getId().equals(actor.getId()))
            throw new BusinessException("Você não pode revogar o seu próprio acesso.");

        if (!actor.getAccessLevel().canManage(target.getAccessLevel())) {
            throw new BusinessException(
                "Você não tem permissão para revogar o acesso de \"" + target.getName() + "\".");
        }

        target.setActive(false);
        userRepository.save(target);
        log.info("User deactivated: {} by {}", target.getEmail(), actor.getEmail());
    }

    /**
     * Alterna o bloqueio de um funcionário (ativo ↔ bloqueado).
     * Funciona tanto para bloquear quanto para desbloquear.
     */
    @Transactional
    public boolean toggleAccess(User actor, UUID targetId) {
        User target = findInOrgAny(actor, targetId);

        if (target.getId().equals(actor.getId()))
            throw new BusinessException("Você não pode bloquear a si mesmo.");

        if (!actor.getAccessLevel().canManage(target.getAccessLevel())) {
            throw new BusinessException(
                "Você não tem permissão para bloquear \"" + target.getName() + "\".");
        }

        boolean newState = !target.getActive();
        target.setActive(newState);
        userRepository.save(target);
        log.info("User toggled active={}: {} by {}", newState, target.getEmail(), actor.getEmail());
        return newState;
    }

    // ── Helpers ──────────────────────────────────────────────

    private void assertCanManageEmployees(User actor) {
        if (!actor.canManageEmployees())
            throw new BusinessException("Você não tem permissão para criar funcionários.");
    }

    private User findInOrg(User actor, UUID targetId) {
        return findInOrgAny(actor, targetId);
    }

    private User findInOrgAny(User actor, UUID targetId) {
        User target = userRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("Funcionário não encontrado"));

        UUID rootId = actor.getRootManagerId();
        boolean inOrg = (target.getManager() != null && target.getManager().getId().equals(rootId))
            || rootId.equals(targetId);

        if (!inOrg)
            throw new BusinessException("Funcionário não pertence à sua organização.");
        return target;
    }
}

