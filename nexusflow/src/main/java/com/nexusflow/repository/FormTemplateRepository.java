package com.nexusflow.repository;

import com.nexusflow.entity.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {

    List<FormTemplate> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<FormTemplate> findByOwnerIdAndActiveTrueOrderByName(UUID ownerId);
}
