package com.nexusflow.repository;

import com.nexusflow.entity.User;
import com.nexusflow.enums.AccessLevel;
import com.nexusflow.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Todos os funcionários ativos de um gerente (diretos). */
    @Query("SELECT u FROM User u WHERE u.manager.id = :mid AND u.active = true ORDER BY u.accessLevel, u.name")
    List<User> findActiveByManager(@Param("mid") UUID managerId);

    /** Funcionários ativos de qualquer nível abaixo de OWNER para um comércio. */
    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
          AND u.accessLevel <> 'OWNER'
          AND (u.manager.id = :mid
               OR u.manager.id IN (
                   SELECT s.id FROM User s WHERE s.manager.id = :mid
               ))
        ORDER BY u.accessLevel, u.name
    """)
    List<User> findAllActiveInOrg(@Param("mid") UUID ownerId);

    /** Conta usuários com determinado nível dentro da organização. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.manager.id = :mid AND u.accessLevel = :level AND u.active = true")
    long countByManagerAndLevel(@Param("mid") UUID managerId, @Param("level") AccessLevel level);

    /** Todos os funcionários (ativos e bloqueados) de um gerente. */
    @Query("SELECT u FROM User u WHERE u.manager.id = :mid AND u.accessLevel <> 'OWNER' ORDER BY u.active DESC, u.accessLevel, u.name")
    List<User> findAllByManager(@Param("mid") UUID managerId);

    /** Todos os owners ativos (para SUPER_ADMIN). */
    List<User> findByRoleAndAccessLevelAndActiveTrueOrderByName(Role role, AccessLevel accessLevel);

    /** Todos os owners, ativos e bloqueados (para SUPER_ADMIN). */
    List<User> findByRoleAndAccessLevelOrderByName(Role role, AccessLevel accessLevel);
}

