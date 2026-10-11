package com.eventflow.controller;

import com.eventflow.dto.ChangePasswordRequest;
import com.eventflow.dto.ProfileUpdateRequest;
import com.eventflow.model.User;
import com.eventflow.service.UserService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;



    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("user", userService.currentUser(authentication));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @ModelAttribute ProfileUpdateRequest request,
                                RedirectAttributes redirectAttributes) {
        User user = userService.currentUser(authentication);
        try {
            userService.updateProfile(user, request);
            redirectAttributes.addFlashAttribute("toastSuccess", "Profile updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/photo")
    public String updateProfilePhoto(Authentication authentication,
                                     @RequestParam("photo") MultipartFile photo,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.currentUser(authentication);
        try {
            userService.uploadProfileImage(user, photo);
            redirectAttributes.addFlashAttribute("toastSuccess", "Profile picture updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/photo/remove")
    public String removeProfilePhoto(Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.currentUser(authentication);
        try {
            userService.removeProfileImage(user);
            redirectAttributes.addFlashAttribute("toastSuccess", "Profile picture removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(Authentication authentication,
                                 @ModelAttribute ChangePasswordRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.currentUser(authentication);
        try {
            userService.changePassword(user, request);
            redirectAttributes.addFlashAttribute("toastSuccess", "Password changed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
