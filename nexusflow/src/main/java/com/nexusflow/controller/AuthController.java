package com.nexusflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null)  model.addAttribute("errorMsg", "E-mail ou senha inválidos.");
        if (logout != null) model.addAttribute("successMsg", "Sessão encerrada com sucesso.");
        return "auth/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
}
