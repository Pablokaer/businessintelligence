package com.nexusflow.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Nível de acesso granular do usuário dentro de um comércio.
 *
 * OWNER      — Gerente/dono: controle total, incluindo outros SUBMANAGER.
 * SUBMANAGER — Subgerente: vê dashboard de todos, cria funcionários, aprova
 *              formulários. NÃO pode alterar OWNER nem outros SUBMANAGER.
 * SENIOR     — Sênior: aprova/rejeita formulários da equipe, mas não gerencia acessos.
 * STANDARD   — Padrão: apenas cria e visualiza os próprios formulários.
 */
@Getter
@RequiredArgsConstructor
public enum AccessLevel {

    PLATFORM  ("Plataforma",    "info",    "🌐",
               false, false, false, false),

    OWNER     ("Proprietário",  "danger",  "👑",
               true,  true,  true,  true),

    SUBMANAGER("Subgerente",    "warning", "🔑",
               true,  true,  true,  false),

    SENIOR    ("Sênior",        "primary", "⭐",
               true,  false, false, false),

    STANDARD  ("Padrão",        "muted",   "👤",
               false, false, false, false);

    /** Rótulo em português para exibição. */
    private final String label;

    /** Classe CSS do badge. */
    private final String badgeClass;

    /** Ícone emoji. */
    private final String icon;

    /** Pode ver a dashboard completa (todos os funcionários). */
    private final boolean canViewAllDashboard;

    /** Pode aprovar/rejeitar formulários. */
    private final boolean canReviewSubmissions;

    /** Pode criar novos funcionários. */
    private final boolean canManageEmployees;

    /** Pode alterar nível de acesso e revogar qualquer conta. */
    private final boolean canManageAccessLevels;

    // ── Helpers ──────────────────────────────────────────────

    /** Retorna true se este nível pode gerenciar (editar/revogar) o alvo. */
    public boolean canManage(AccessLevel target) {
        if (target == PLATFORM) return false;
        if (!canManageAccessLevels) return false;
        // OWNER pode gerenciar todos exceto outro OWNER (multi-owner não permitido)
        if (this == OWNER)      return target != OWNER;
        // SUBMANAGER não pode gerenciar OWNER nem outros SUBMANAGER
        if (this == SUBMANAGER) return target != OWNER && target != SUBMANAGER;
        return false;
    }

    /** Retorna true se este nível pode atribuir o nível alvo a alguém. */
    public boolean canAssign(AccessLevel target) {
        if (!canManageAccessLevels) return false;
        // OWNER e PLATFORM nunca atribuíveis via interface
        if (target == OWNER || target == PLATFORM) return false;
        // OWNER pode atribuir SUBMANAGER, SENIOR, STANDARD
        if (this == OWNER) return true;
        // SUBMANAGER só pode atribuir SENIOR e STANDARD (não pode criar outro SUBMANAGER)
        if (this == SUBMANAGER) return target == SENIOR || target == STANDARD;
        return false;
    }

    /** Níveis que o usuário atual pode exibir no select de "nível de acesso". */
    public static AccessLevel[] assignableBy(AccessLevel actor) {
        return java.util.Arrays.stream(values())
            .filter(t -> actor.canAssign(t))
            .toArray(AccessLevel[]::new);
    }
}
