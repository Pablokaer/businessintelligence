package com.nexusflow.controller;

import com.nexusflow.dto.FormDTOs;
import com.nexusflow.entity.FormSubmission;
import com.nexusflow.exception.NotFoundException;
import com.nexusflow.service.FormSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.UUID;

@Controller
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestFormController {

    private final FormSubmissionService formSubmissionService;

    @GetMapping("/form/{token}")
    public String showForm(@PathVariable UUID token, Model model) {
        try {
            FormSubmission draft = formSubmissionService.findDraftByToken(token);

            FormDTOs.GuestFillForm form = new FormDTOs.GuestFillForm();
            var responses = new ArrayList<FormDTOs.FieldResponseItem>();
            draft.getTemplate().getFields().forEach(f -> {
                var item = new FormDTOs.FieldResponseItem();
                item.setFieldId(f.getId().toString());
                responses.add(item);
            });
            form.setResponses(responses);

            model.addAttribute("draft", draft);
            model.addAttribute("form",  form);
            return "guest/form";
        } catch (Exception e) {
            model.addAttribute("reason", e.getMessage());
            return "guest/unavailable";
        }
    }

    @PostMapping("/form/{token}")
    public String submitForm(@PathVariable UUID token,
                             @ModelAttribute("form") FormDTOs.GuestFillForm form,
                             Model model,
                             RedirectAttributes ra) {
        try {
            formSubmissionService.submitGuestResponses(token, form);
            return "redirect:/guest/form/" + token + "/obrigado";
        } catch (NotFoundException e) {
            model.addAttribute("reason", e.getMessage());
            return "guest/unavailable";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/guest/form/" + token;
        }
    }

    @GetMapping("/form/{token}/obrigado")
    public String thankYou(Model model) {
        return "guest/thank-you";
    }
}
