package com.eventflow.controller;

import com.eventflow.config.SecurityConfig;
import lombok.RequiredArgsConstructor;
import com.eventflow.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import com.eventflow.dto.ForgotPasswordRequest;
import com.eventflow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService users;

    @Value("${eventflow.demo-access.enabled:true}")
    private boolean demoAccessEnabled;


    @GetMapping({"/login", "/register"})
    public String authPage(Authentication authentication, @RequestParam(required = false) String error,
                           @RequestParam(required = false) String logout, @RequestParam(required = false) String redirect,
                           @RequestParam(required = false) String forgot,
                           HttpServletRequest request, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) return "redirect:/dashboard";
        if (error != null) model.addAttribute("loginError", "Invalid email or password.");
        if (logout != null) model.addAttribute("logoutSuccess", "You have been signed out successfully.");
        if ("/register".equals(request.getRequestURI())) model.addAttribute("activeTab", "register");
        model.addAttribute("redirect", SecurityConfig.safeRedirect(redirect));
        model.addAttribute("demoAccessAvailable", demoAvailable(request));
        model.addAttribute("forgotOpen", forgot != null);
        return "auth";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request, BindingResult bindingResult,
                           @RequestParam(required = false) String redirect,
                           RedirectAttributes flash) {
        String name = request.fullName() == null ? "" : request.fullName().trim().replaceAll("\\s+", " ");
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String role = request.role() == null ? "" : request.role().trim().toUpperCase(Locale.ROOT);
        flash.addFlashAttribute("formName", name); flash.addFlashAttribute("formEmail", email);
        flash.addFlashAttribute("formRole", role);
        String safe = SecurityConfig.safeRedirect(redirect);
        if (safe != null) flash.addAttribute("redirect", safe);
        if (bindingResult.hasErrors()) {
            flash.addFlashAttribute("registerError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            flash.addFlashAttribute("activeTab", "register");
            return "redirect:/register";
        }
        try {
            users.register(request);
            flash.addFlashAttribute("registerSuccess", "Account created. Sign in to continue.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("registerError", ex.getMessage());
            flash.addFlashAttribute("activeTab", "register");
            return "redirect:/register";
        }
    }

    // Original EventFlow recovery flow: check email, then reset in the login dialog.
    // This does not require Jakarta Mail or SMTP.
    @GetMapping("/forgot-password")
    public String forgotPasswordEntry() {
        return "redirect:/login?forgot=1";
    }

    @PostMapping("/forgot-password/check")
    public String checkForgotPasswordEmail(@ModelAttribute ForgotPasswordRequest request,
                                            RedirectAttributes flash) {
        String cleanEmail = request.email() == null
                ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        flash.addFlashAttribute("forgotEmail", cleanEmail);
        try {
            users.validatePasswordResetEmail(cleanEmail);
            flash.addFlashAttribute("forgotStep", "reset");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("forgotStep", "email");
            flash.addFlashAttribute("forgotError", ex.getMessage());
        }
        return "redirect:/login";
    }

    @PostMapping("/forgot-password/reset")
    public String resetForgottenPassword(@ModelAttribute ForgotPasswordRequest request,
                                          RedirectAttributes flash) {
        String cleanEmail = request.email() == null
                ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        try {
            users.resetForgottenPassword(cleanEmail,
                    request.newPassword(), request.confirmPassword());
            flash.addFlashAttribute("passwordResetSuccess",
                    "Password updated successfully. Sign in with your new password.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("forgotEmail", cleanEmail);
            flash.addFlashAttribute("forgotStep", "reset");
            flash.addFlashAttribute("forgotError", ex.getMessage());
        }
        return "redirect:/login";
    }
    // Show the same Quick Access options on localhost and hosted deployments.
    // The existing demo-access flag still controls whether this feature is enabled.
    private boolean demoAvailable(HttpServletRequest request) {
        return demoAccessEnabled;
    }

}
