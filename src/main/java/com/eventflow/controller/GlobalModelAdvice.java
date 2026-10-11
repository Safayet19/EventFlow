package com.eventflow.controller;

import lombok.RequiredArgsConstructor;
import com.eventflow.model.User;
import com.eventflow.service.ContactRequestService;
import com.eventflow.service.NotificationService;
import com.eventflow.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserService userService;
    private final NotificationService notificationService;
    private final ContactRequestService contactRequestService;



    @ModelAttribute
    public void addCommonData(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        User user = userService.currentUser(authentication);
        model.addAttribute("currentUser", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
        if ("ADMIN".equals(user.getRole())) {
            model.addAttribute("newContactRequestCount", contactRequestService.countNew());
        }
    }
}
