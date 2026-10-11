package com.eventflow.controller;

import com.eventflow.dto.ContactRequestForm;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import com.eventflow.model.ContactRequest;
import com.eventflow.model.User;
import com.eventflow.service.ContactRequestService;
import com.eventflow.service.UserService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ContactController {

    private final ContactRequestService contactRequestService;
    private final UserService userService;



    @GetMapping("/contact")
    public String contact(Authentication authentication, Model model) {
        boolean signedIn = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (signedIn) {
            User user = userService.currentUser(authentication);
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin/contact-requests";
            }
        }

        model.addAttribute("signedIn", signedIn);
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid ContactRequestForm form, BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            User user = userService.currentUser(authentication);
            if ("ADMIN".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("toastError", "Administrators manage support requests from Contact Requests.");
                return "redirect:/admin/contact-requests";
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("contactError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            redirectAttributes.addFlashAttribute("contactForm", form);
            return "redirect:/contact";
        }
        try {
            contactRequestService.submit(form);
            redirectAttributes.addFlashAttribute(
                    "contactSuccess",
                    "Your message has been sent. EventFlow support will review it shortly."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("contactError", ex.getMessage());
            redirectAttributes.addFlashAttribute("contactForm", form);
        }
        return "redirect:/contact";
    }

    @GetMapping("/admin/contact-requests")
    public String adminContactRequests(Model model) {
        List<ContactRequest> requests = contactRequestService.findAll();
        model.addAttribute("contactRequests", requests);
        model.addAttribute("newRequestCount", contactRequestService.countByStatus("NEW"));
        model.addAttribute("inProgressRequestCount", contactRequestService.countByStatus("IN_PROGRESS"));
        model.addAttribute("resolvedRequestCount", contactRequestService.countByStatus("RESOLVED"));
        return "admin-contact-requests";
    }

    @PostMapping("/admin/contact-requests/{id}/update")
    public String updateContactRequest(@PathVariable String id,
                                       @RequestParam String status,
                                       @RequestParam(required = false) String adminNote,
                                       RedirectAttributes redirectAttributes) {
        try {
            contactRequestService.update(id, status, adminNote);
            redirectAttributes.addFlashAttribute("toastSuccess", "Contact request updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/contact-requests";
    }

    @PostMapping("/admin/contact-requests/{id}/delete")
    public String deleteContactRequest(@PathVariable String id,
                                       RedirectAttributes redirectAttributes) {
        try {
            contactRequestService.delete(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Contact request deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/admin/contact-requests";
    }
}
