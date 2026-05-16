package com.nexusflow.config;

import com.nexusflow.entity.User;
import com.nexusflow.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.UUID;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SubmissionRepository submissionRepo;

    @ModelAttribute("pendingCount")
    public long pendingCount(@AuthenticationPrincipal User user) {
        if (user == null || !user.hasManagerView()) return 0L;
        UUID rootId = user.getRootManagerId();
        if (rootId == null) return 0L;
        return submissionRepo.countPendingByManager(rootId);
    }
}
