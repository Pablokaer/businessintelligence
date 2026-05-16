package com.nexusflow.controller;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.User;
import com.nexusflow.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) List<UUID> ownerIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = to   != null ? to   : LocalDate.now();
        List<User> owners = adminService.listOwners();
        List<UUID> selected = (ownerIds != null && !ownerIds.isEmpty())
            ? ownerIds
            : owners.stream().map(User::getId).toList();

        model.addAttribute("owners",           owners);
        model.addAttribute("selectedOwnerIds", selected);
        model.addAttribute("ownerStats",       adminService.statsPerOwner(selected, f, t));
        model.addAttribute("compiled",         adminService.compiledSummary(selected, f, t));
        model.addAttribute("from",             f);
        model.addAttribute("to",               t);
        model.addAttribute("activeMenu",       "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/owners")
    public String owners(Model model) {
        model.addAttribute("owners",     adminService.listOwners());
        model.addAttribute("form",       new FormDTOs.CreateOwnerForm());
        model.addAttribute("activeMenu", "owners");
        return "admin/owners";
    }

    @PostMapping("/owners")
    public String createOwner(@Valid @ModelAttribute("form") FormDTOs.CreateOwnerForm form,
                              BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("owners",     adminService.listOwners());
            model.addAttribute("activeMenu", "owners");
            return "admin/owners";
        }
        try {
            adminService.createOwner(form);
            ra.addFlashAttribute("successMsg", "Owner \"" + form.getName() + "\" criado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/owners";
    }

    @PostMapping("/owners/{id}/toggle")
    public String toggleOwner(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            adminService.toggleOwnerAccess(id);
            ra.addFlashAttribute("successMsg", "Acesso do owner atualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/owners";
    }

    @PostMapping("/owners/{id}/password")
    public String changePassword(@PathVariable UUID id,
                                 @Valid @ModelAttribute FormDTOs.ChangeOwnerPasswordForm form,
                                 BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Senha inválida: mínimo 4 caracteres.");
            return "redirect:/admin/owners";
        }
        try {
            adminService.changeOwnerPassword(id, form);
            ra.addFlashAttribute("successMsg", "Senha alterada com sucesso.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/owners";
    }
}
