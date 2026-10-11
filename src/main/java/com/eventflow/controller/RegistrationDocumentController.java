package com.eventflow.controller;

import com.eventflow.model.Event;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.RegistrationRepository;
import com.eventflow.service.RegistrationDocumentService;
import com.eventflow.service.RegistrationRules;
import com.eventflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class RegistrationDocumentController {
    private final UserService userService;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final RegistrationDocumentService documents;

    @GetMapping("/participant/registrations/{registrationId}/ticket.pdf")
    public ResponseEntity<byte[]> ticket(@PathVariable String registrationId,
                                         Authentication authentication) {
        Registration registration = ownedRegistration(registrationId, authentication);
        Event event = activeEvent(registration);
        try {
            return download(documents.ticket(registration, event), "eventflow-ticket-", registration);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create ticket.", ex);
        }
    }

    @GetMapping("/participant/registrations/{registrationId}/receipt.pdf")
    public ResponseEntity<byte[]> receipt(@PathVariable String registrationId,
                                          Authentication authentication) {
        Registration registration = ownedRegistration(registrationId, authentication);
        if (!RegistrationRules.paidTicket(registration)
                || registration.getPaymentStatus() == null
                || !registration.getPaymentStatus().startsWith("PAID")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No paid receipt for this registration.");
        }
        Event event = activeEvent(registration);
        try {
            return download(documents.receipt(registration, event), "eventflow-receipt-", registration);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create receipt.", ex);
        }
    }

    private Registration ownedRegistration(String id, Authentication authentication) {
        User user = userService.currentUser(authentication);
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user == null || !user.getId().equals(registration.getParticipantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return registration;
    }

    private Event activeEvent(Registration registration) {
        return eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not available."));
    }

    private ResponseEntity<byte[]> download(byte[] content, String prefix, Registration registration) {
        String reference = registration.getPaymentReference() != null
                ? registration.getPaymentReference() : registration.getId();
        String safe = reference == null ? "registration" : reference.replaceAll("[^A-Za-z0-9_-]", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + prefix + safe + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .body(content);
    }
}
