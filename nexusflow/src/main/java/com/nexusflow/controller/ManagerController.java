package com.nexusflow.controller;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.FormSubmission;
import com.nexusflow.entity.FormTemplate;
import com.nexusflow.entity.Submission;
import com.nexusflow.entity.User;
import com.nexusflow.enums.AccessLevel;
import com.nexusflow.enums.FieldType;
import com.nexusflow.enums.SubmissionStatus;
import com.nexusflow.enums.SubmissionType;
import com.nexusflow.service.EmployeeService;
import com.nexusflow.service.FormSubmissionService;
import com.nexusflow.service.FormTemplateService;
import com.nexusflow.service.ReportService;
import com.nexusflow.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final EmployeeService employeeService;
    private final SubmissionService submissionService;
    private final ReportService reportService;
    private final FormTemplateService formTemplateService;
    private final FormSubmissionService formSubmissionService;

    private static final DateTimeFormatter CHART_FMT_WEEKDAY =
        DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter CHART_FMT_DATE =
        DateTimeFormatter.ofPattern("dd/MM");

    // ── Dashboard ─────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User actor, Model model) {
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to   = LocalDate.now();
        UUID rootId    = actor.getRootManagerId();

        List<com.nexusflow.dto.ViewModels.DailySaleVM> dailySales =
            reportService.dailySales(rootId, from.minusDays(6), to);

        model.addAttribute("summary",       reportService.summary(rootId, from, to));
        model.addAttribute("pending",       submissionService.listPending(actor));
        model.addAttribute("employees",     employeeService.listVisible(actor));
        model.addAttribute("byEmployee",    reportService.byEmployee(rootId, from, to));
        model.addAttribute("dailySales",    dailySales);
        model.addAttribute("dailySalesJson", buildChartJson(dailySales, CHART_FMT_WEEKDAY));
        model.addAttribute("activeMenu",    "dashboard");
        return "manager/dashboard";
    }

    // ── Funcionários ──────────────────────────────────────────
    @GetMapping("/employees")
    public String employees(@AuthenticationPrincipal User actor, Model model) {
        model.addAttribute("employees",        employeeService.listVisible(actor));
        model.addAttribute("form",             new FormDTOs.CreateEmployeeForm());
        model.addAttribute("assignableLevels", AccessLevel.assignableBy(actor.getAccessLevel()));
        model.addAttribute("allLevels",        AccessLevel.values());
        model.addAttribute("comercioSlug",     ownerDomainSlug(actor));
        model.addAttribute("actor",            actor);
        model.addAttribute("activeMenu",       "employees");
        return "manager/employees";
    }

    @PostMapping("/employees")
    public String createEmployee(@AuthenticationPrincipal User actor,
                                 @Valid @ModelAttribute("form") FormDTOs.CreateEmployeeForm form,
                                 BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("employees",        employeeService.listVisible(actor));
            model.addAttribute("assignableLevels", AccessLevel.assignableBy(actor.getAccessLevel()));
            model.addAttribute("allLevels",        AccessLevel.values());
            model.addAttribute("comercioSlug",     ownerDomainSlug(actor));
            model.addAttribute("actor",            actor);
            model.addAttribute("activeMenu",       "employees");
            return "manager/employees";
        }
        try {
            employeeService.create(actor, form);
            ra.addFlashAttribute("successMsg", "Funcionário criado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/employees";
    }

    /** Altera o nível de acesso de um funcionário. */
    @PostMapping("/employees/{id}/access")
    public String updateAccess(@AuthenticationPrincipal User actor,
                               @PathVariable UUID id,
                               @Valid @ModelAttribute FormDTOs.UpdateAccessForm form,
                               RedirectAttributes ra) {
        try {
            User updated = employeeService.updateAccessLevel(actor, id, form);
            ra.addFlashAttribute("successMsg",
                "Nível de \"" + updated.getName() + "\" alterado para " + updated.getAccessLevel().getLabel() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/employees";
    }

    @PostMapping("/employees/{id}/deactivate")
    public String deactivateEmployee(@AuthenticationPrincipal User actor,
                                     @PathVariable UUID id, RedirectAttributes ra) {
        try {
            employeeService.deactivate(actor, id);
            ra.addFlashAttribute("successMsg", "Acesso revogado permanentemente.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/employees";
    }

    @PostMapping("/employees/{id}/toggle")
    public String toggleEmployee(@AuthenticationPrincipal User actor,
                                 @PathVariable UUID id, RedirectAttributes ra) {
        try {
            boolean active = employeeService.toggleAccess(actor, id);
            ra.addFlashAttribute("successMsg", active ? "Funcionário desbloqueado." : "Funcionário bloqueado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/employees";
    }

    // ── Formulários ───────────────────────────────────────────
    @GetMapping("/submissions")
    public String submissions(@AuthenticationPrincipal User actor,
                              @ModelAttribute FormDTOs.SubmissionFilter filter,
                              Model model) {
        Page<Submission> page = submissionService.list(actor, filter);
        model.addAttribute("page",       page);
        model.addAttribute("filter",     filter);
        model.addAttribute("employees",  employeeService.listVisible(actor));
        model.addAttribute("types",      SubmissionType.values());
        model.addAttribute("statuses",   SubmissionStatus.values());
        model.addAttribute("actor",      actor);
        model.addAttribute("activeMenu", "submissions");
        return "manager/submissions";
    }

    @GetMapping("/submissions/{id}")
    public String submissionDetail(@AuthenticationPrincipal User actor,
                                   @PathVariable UUID id, Model model) {
        model.addAttribute("submission", submissionService.findById(actor, id));
        model.addAttribute("reviewForm", new FormDTOs.ReviewForm());
        model.addAttribute("actor",      actor);
        model.addAttribute("activeMenu", "submissions");
        return "manager/submission-detail";
    }

    @PostMapping("/submissions/{id}/review")
    public String reviewSubmission(@AuthenticationPrincipal User actor,
                                   @PathVariable UUID id,
                                   @Valid @ModelAttribute("reviewForm") FormDTOs.ReviewForm form,
                                   BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) return "redirect:/manager/submissions/" + id;
        try {
            submissionService.review(actor, id, form);
            ra.addFlashAttribute("successMsg",
                form.getStatus() == SubmissionStatus.APPROVED ? "Formulário aprovado!" : "Formulário rejeitado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/submissions";
    }

    // ── Formulários dinâmicos (templates) ────────────────────────
    @GetMapping("/templates")
    public String templates(@AuthenticationPrincipal User actor, Model model) {
        model.addAttribute("templates",  formTemplateService.listByManager(actor));
        model.addAttribute("form",       new FormDTOs.CreateTemplateForm());
        model.addAttribute("activeMenu", "templates");
        return "manager/templates";
    }

    @PostMapping("/templates")
    public String createTemplate(@AuthenticationPrincipal User actor,
                                 @Valid @ModelAttribute("form") FormDTOs.CreateTemplateForm form,
                                 BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("templates",  formTemplateService.listByManager(actor));
            model.addAttribute("activeMenu", "templates");
            return "manager/templates";
        }
        try {
            FormTemplate t = formTemplateService.create(actor, form);
            ra.addFlashAttribute("successMsg", "Formulário \"" + t.getName() + "\" criado!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/templates";
    }

    @GetMapping("/templates/{id}")
    public String templateDetail(@PathVariable UUID id, Model model) {
        FormTemplate t = formTemplateService.findById(id);
        model.addAttribute("template",   t);
        model.addAttribute("fieldForm",  new FormDTOs.AddFieldForm());
        model.addAttribute("fieldTypes", FieldType.values());
        model.addAttribute("activeMenu", "templates");
        return "manager/template-detail";
    }

    @PostMapping("/templates/{id}/fields")
    public String addField(@PathVariable UUID id,
                           @Valid @ModelAttribute("fieldForm") FormDTOs.AddFieldForm form,
                           BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Dados inválidos para o campo.");
            return "redirect:/manager/templates/" + id;
        }
        try {
            formTemplateService.addField(id, form);
            ra.addFlashAttribute("successMsg", "Campo adicionado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/templates/" + id;
    }

    @PostMapping("/templates/{id}/fields/{fieldId}/remove")
    public String removeField(@PathVariable UUID id, @PathVariable UUID fieldId, RedirectAttributes ra) {
        try {
            formTemplateService.removeField(id, fieldId);
            ra.addFlashAttribute("successMsg", "Campo removido.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/templates/" + id;
    }

    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            formTemplateService.delete(id);
            ra.addFlashAttribute("successMsg", "Formulário eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/templates";
    }

    @PostMapping("/templates/{id}/toggle")
    public String toggleTemplate(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            FormTemplate t = formTemplateService.toggleActive(id);
            ra.addFlashAttribute("successMsg", t.getActive() ? "Formulário ativado." : "Formulário desativado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/templates/" + id;
    }

    // ── Registros de formulários dinâmicos ────────────────────
    @GetMapping("/form-submissions")
    public String formSubmissions(@AuthenticationPrincipal User actor,
                                  @ModelAttribute FormDTOs.FormSubmissionFilter filter,
                                  Model model) {
        Page<FormSubmission> page = formTemplateService.listAllSubmissions(actor, filter);
        model.addAttribute("page",       page);
        model.addAttribute("filter",     filter);
        model.addAttribute("statuses",   SubmissionStatus.values());
        model.addAttribute("templates",  formTemplateService.listByManager(actor));
        model.addAttribute("activeMenu", "form-submissions");
        return "manager/form-submissions";
    }

    @GetMapping("/form-submissions/{id}")
    public String formSubmissionDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("submission", formSubmissionService.findById(id));
        model.addAttribute("reviewForm", new FormDTOs.ReviewFormSubmissionForm());
        model.addAttribute("statuses",   SubmissionStatus.values());
        model.addAttribute("activeMenu", "form-submissions");
        return "manager/form-submission-detail";
    }

    @PostMapping("/form-submissions/{id}/review")
    public String reviewFormSubmission(@PathVariable UUID id,
                                       @AuthenticationPrincipal User actor,
                                       @Valid @ModelAttribute("reviewForm") FormDTOs.ReviewFormSubmissionForm form,
                                       BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) return "redirect:/manager/form-submissions/" + id;
        try {
            formSubmissionService.review(actor, id, form);
            ra.addFlashAttribute("successMsg",
                form.getStatus() == SubmissionStatus.APPROVED ? "Registro aprovado!" : "Registro rejeitado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/form-submissions";
    }

    // ── Relatórios ────────────────────────────────────────────
    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal User actor,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        LocalDate f  = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t  = to   != null ? to   : LocalDate.now();
        UUID rootId  = actor.getRootManagerId();

        List<com.nexusflow.dto.ViewModels.DailySaleVM> dailySales = reportService.dailySales(rootId, f, t);

        model.addAttribute("summary",          reportService.summary(rootId, f, t));
        model.addAttribute("byCategory",       reportService.byCategory(rootId, f, t));
        model.addAttribute("byEmployee",       reportService.byEmployee(rootId, f, t));
        model.addAttribute("dailySales",       dailySales);
        model.addAttribute("dailySalesChartJson", buildChartJson(dailySales, CHART_FMT_DATE));
        model.addAttribute("from",             f);
        model.addAttribute("to",               t);
        model.addAttribute("activeMenu",       "reports");
        return "manager/reports";
    }

    private static String buildChartJson(List<com.nexusflow.dto.ViewModels.DailySaleVM> sales,
                                         DateTimeFormatter labelFmt) {
        if (sales.isEmpty()) return "[]";
        return sales.stream()
            .map(d -> {
                String lbl    = d.getDate().format(labelFmt);
                String fmtVal = "€ " + String.format("%.2f", d.getValue()).replace(".", ",");
                return "{\"v\":" + d.getValue() + ",\"label\":\"" + lbl + "\",\"formatted\":\"" + fmtVal + "\"}";
            })
            .collect(Collectors.joining(",", "[", "]"));
    }

    /** Extrai o slug do domínio do e-mail do actor (ex: "joao@padaria.com" → "padaria"). */
    private static String ownerDomainSlug(User actor) {
        String email = actor.getEmail();
        String domain = email.substring(email.indexOf('@') + 1);
        int dot = domain.lastIndexOf('.');
        return dot > 0 ? domain.substring(0, dot) : domain;
    }
}

