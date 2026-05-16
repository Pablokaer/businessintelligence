package com.nexusflow.entity;

import com.nexusflow.enums.AccessLevel;
import com.nexusflow.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String comercio;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Papel técnico: MANAGER (dono do comércio) ou EMPLOYEE (qualquer colaborador).
     * Usado pelo Spring Security para proteger rotas via hasRole().
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "user_role")
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class)
    private Role role;

    /**
     * Nível de acesso granular dentro da hierarquia do comércio.
     * OWNER = gerente/dono. Demais níveis são funcionários com permissões variadas.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", length = 20, nullable = false)
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.STANDARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> employees = new ArrayList<>();

    @Builder.Default
    private Boolean active = true;

    @CreatedDate @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ── UserDetails ──────────────────────────────────────────

    /**
     * Spring Security authorities.
     * MANAGER → ROLE_MANAGER
     * EMPLOYEE com canViewAllDashboard → ROLE_MANAGER (acessa /manager/**)
     * EMPLOYEE padrão → ROLE_EMPLOYEE
     *
     * Isso permite que SUBMANAGER e SENIOR acessem as rotas /manager/**
     * sem serem donos do comércio.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role == Role.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            return authorities;
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (role == Role.EMPLOYEE && accessLevel.isCanViewAllDashboard()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
        }
        authorities.add(new SimpleGrantedAuthority("ACCESS_" + accessLevel.name()));
        return authorities;
    }

    @Override public String getPassword()   { return passwordHash; }
    @Override public String getUsername()   { return email; }
    @Override public boolean isEnabled()    { return active; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    // ── Helpers de negócio ───────────────────────────────────

    public String getInitials() {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /** True apenas para o dono real do comércio. */
    public boolean isOwner()      { return accessLevel == AccessLevel.OWNER; }

    /** True para quem enxerga a dashboard gerencial (OWNER + SUBMANAGER). */
    public boolean hasManagerView() { return accessLevel.isCanViewAllDashboard(); }

    /** True para quem pode aprovar formulários. */
    public boolean canReview()    { return accessLevel.isCanReviewSubmissions(); }

    /** True para quem pode criar funcionários. */
    public boolean canManageEmployees() { return accessLevel.isCanManageEmployees(); }

    /**
     * Resolve o UUID "raiz" do gerente dono para queries de equipe.
     * OWNER retorna o próprio ID; demais retornam o ID do seu manager.
     */
    public UUID getRootManagerId() {
        if (role == Role.SUPER_ADMIN) return null;
        if (isOwner()) return id;
        return manager != null ? manager.getId() : id;
    }
}

