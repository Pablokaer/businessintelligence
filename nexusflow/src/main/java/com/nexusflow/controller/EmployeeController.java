package com.nexusflow.controller;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.FormSubmission;
import com.nexusflow.entity.FormTemplate;
import com.nexusflow.entity.Submission;
import com.nexusflow.entity.User;
import com.nexusflow.enums.SubmissionType;
import com.nexusflow.service.FormSubmissionService;
import com.nexusflow.service.FormTemplateService;
import com.nexusflow.service.ReportService;
import com.nexusflow.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final SubmissionService submissionService;
    private final ReportService reportService;
    private final FormTemplateService formTemplateService;
    private final FormSubmissionService formSubmissionService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User employee, Model model) {
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to   = LocalDate.now();

        var filter = new FormDTOs.SubmissionFilter();
        filter.setSize(5);
        Page<Submission> recent = submissionService.list(employee, filter);

        model.addAttribute("summary", reportService.summaryForEmployee(employee.getId(), from, to));
        model.addAttribute("recentSubmissions", recent.getContent());
        model.addAttribute("activeMenu", "dashboard");
        return "employee/dashboard";
    }

    @GetMapping("/submissions")
    public String submissions(@AuthenticationPrincipal User employee,
                              @ModelAttribute FormDTOs.SubmissionFilter filter,
                              Model model) {
        Page<Submission> page = submissionService.list(employee, filter);
        model.addAttribute("page", page);
        model.addAttribute("filter", filter);
        model.addAttribute("activeMenu", "submissions");
        return "employee/submissions";
    }

    @GetMapping("/submissions/new")
    public String newForm(Model model) {
        model.addAttribute("form", new FormDTOs.SubmissionForm());
        model.addAttribute("types", SubmissionType.values());
        model.addAttribute("activeMenu", "new");
        return "employee/submission-form";
    }

    @PostMapping("/submissions/new")
    public String submitForm(@AuthenticationPrincipal User employee,
                             @Valid @ModelAttribute("form") FormDTOs.SubmissionForm form,
                             BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("types", SubmissionType.values());
            model.addAttribute("activeMenu", "new");
            return "employee/submission-form";
        }
        Submission saved = submissionService.create(employee, form);
        ra.addFlashAttribute("successMsg", "Formulário " + saved.getFormNumber() + " enviado!");
        return "redirect:/employee/submissions";
    }

    @GetMapping("/submissions/{id}")
    public String detail(@AuthenticationPrincipal User employee, @PathVariable UUID id, Model model) {
        model.addAttribute("submission", submissionService.findById(employee, id));
        model.addAttribute("activeMenu", "submissions");
        return "employee/submission-detail";
    }

    // ── Formulários dinâmicos ─────────────────────────────────
    @GetMapping("/forms")
    public String forms(@AuthenticationPrincipal User employee, Model model) {
        model.addAttribute("templates",  formTemplateService.listActiveForEmployee(employee));
        model.addAttribute("activeMenu", "forms");
        return "employee/forms";
    }

    @GetMapping("/forms/{templateId}/fill")
    public String fillForm(@AuthenticationPrincipal User employee,
                           @PathVariable UUID templateId, Model model) {
        FormTemplate template = formTemplateService.findById(templateId);
        FormDTOs.FormFillForm form = new FormDTOs.FormFillForm();
        List<FormDTOs.FieldResponseItem> responses = new ArrayList<>();
        template.getFields().forEach(f -> {
            FormDTOs.FieldResponseItem item = new FormDTOs.FieldResponseItem();
            item.setFieldId(f.getId().toString());
            responses.add(item);
        });
        form.setResponses(responses);
        model.addAttribute("template", template);
        model.addAttribute("form",     form);
        model.addAttribute("activeMenu", "forms");
        return "employee/form-fill";
    }

    @PostMapping("/forms/{templateId}/fill")
    public String submitFillForm(@AuthenticationPrincipal User employee,
                                 @PathVariable UUID templateId,
                                 @Valid @ModelAttribute("form") FormDTOs.FormFillForm form,
                                 BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("template", formTemplateService.findById(templateId));
            model.addAttribute("activeMenu", "forms");
            return "employee/form-fill";
        }
        try {
            formSubmissionService.submit(employee, templateId, form);
            ra.addFlashAttribute("successMsg", "Registro enviado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/employee/form-submissions";
    }

    @GetMapping("/form-submissions")
    public String myFormSubmissions(@AuthenticationPrincipal User employee,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {
        model.addAttribute("page",       formSubmissionService.listByEmployee(employee, page, 20));
        model.addAttribute("activeMenu", "form-submissions");
        return "employee/form-submissions";
    }

    @GetMapping("/form-submissions/{id}")
    public String formSubmissionDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("submission", formSubmissionService.findById(id));
        model.addAttribute("activeMenu", "form-submissions");
        return "employee/form-submission-detail";
    }

    // ── Rascunhos compartilhados ──────────────────────────────
    @PostMapping("/forms/{templateId}/draft")
    public String createDraft(@AuthenticationPrincipal User employee,
                              @PathVariable UUID templateId,
                              @Valid @ModelAttribute("serviceData") FormDTOs.ServiceDataForm form,
                              BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Preencha todos os dados do serviço antes de gerar o link.");
            return "redirect:/employee/forms/" + templateId + "/fill";
        }
        try {
            FormSubmission draft = formSubmissionService.createDraft(employee, templateId, form);
            return "redirect:/employee/forms/draft/" + draft.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/employee/forms/" + templateId + "/fill";
        }
    }

    @GetMapping("/forms/draft/{draftId}")
    public String viewDraft(@AuthenticationPrincipal User employee,
                            @PathVariable UUID draftId,
                            HttpServletRequest request,
                            Model model) {
        FormSubmission draft = formSubmissionService.findById(draftId);
        String base = request.getScheme() + "://" + request.getServerName()
                      + (request.getServerPort() != 80 && request.getServerPort() != 443
                         ? ":" + request.getServerPort() : "");
        model.addAttribute("draft",       draft);
        model.addAttribute("shareUrl",    base + "/guest/form/" + draft.getShareToken());
        model.addAttribute("clientFilled", !draft.getResponses().isEmpty());
        model.addAttribute("activeMenu",  "forms");
        return "employee/form-draft";
    }

    @PostMapping("/form-submissions/{id}/delete")
    public String deleteSubmission(@AuthenticationPrincipal User employee,
                                   @PathVariable UUID id, RedirectAttributes ra) {
        try {
            formSubmissionService.deletePending(employee, id);
            ra.addFlashAttribute("successMsg", "Registro excluído com sucesso.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/employee/form-submissions";
    }

    @GetMapping("/forms/draft/{draftId}/status")
    @ResponseBody
    public Map<String, Boolean> draftStatus(@PathVariable UUID draftId) {
        return Map.of("filled", formSubmissionService.isDraftFilledByGuest(draftId));
    }

    @PostMapping("/forms/draft/{draftId}/submit")
    public String submitDraft(@PathVariable UUID draftId, RedirectAttributes ra) {
        try {
            formSubmissionService.submitDraft(draftId);
            ra.addFlashAttribute("successMsg", "Formulário submetido para aprovação!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/employee/form-submissions";
    }
}
