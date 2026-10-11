package com.eventflow.service;

import com.eventflow.dto.ChangePasswordRequest;
import com.eventflow.dto.ProfileUpdateRequest;
import com.eventflow.dto.RegisterRequest;
import com.eventflow.model.Event;
import com.eventflow.model.Feedback;
import com.eventflow.model.ProfileImage;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.repository.CouponRepository;
import com.eventflow.repository.EventImageRepository;
import com.eventflow.repository.EventTicketTypeRepository;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.FeedbackRepository;
import com.eventflow.repository.ProfileImageRepository;
import com.eventflow.repository.RegistrationRepository;
import com.eventflow.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> DEMO_EMAILS = Set.of(
            "admin@eventflow.com", "organizer@eventflow.com", "participant@eventflow.com");

    /** Demo-account protections belong to the account service, not MongoDB entities. */
    public boolean isDemoAccount(User user) {
        return user != null && user.getEmail() != null
                && DEMO_EMAILS.contains(user.getEmail().trim().toLowerCase(Locale.ROOT));
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}' .-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private static final Set<String> REGISTER_ROLES = Set.of("PARTICIPANT", "ORGANIZER");
    private static final Set<String> ALL_ROLES = Set.of("PARTICIPANT", "ORGANIZER", "ADMIN");
    private static final Set<String> PROFILE_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_PROFILE_IMAGE_SIZE = 2 * 1024 * 1024;

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventImageRepository eventImageRepository;
    private final ProfileImageRepository profileImageRepository;
    private final RegistrationRepository registrationRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final EventProgramService eventProgramService;
    private final CouponRepository couponRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final EventGalleryService eventGalleryService;
    private final PasswordEncoder passwordEncoder;



    public User register(RegisterRequest request) {
        String cleanName = cleanName(request.fullName());
        String cleanEmail = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String cleanRole = request.role() == null ? "" : request.role().trim().toUpperCase(Locale.ROOT);
        String password = request.password() == null ? "" : request.password();
        String confirmPassword = request.confirmPassword() == null ? "" : request.confirmPassword();

        if (!NAME_PATTERN.matcher(cleanName).matches()) {
            throw new IllegalArgumentException("Enter a valid full name (3-50 characters).");
        }
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!REGISTER_ROLES.contains(cleanRole)) {
            throw new IllegalArgumentException("Choose Participant or Organizer.");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must have 8+ characters, uppercase, lowercase, number and special character."
            );
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User newUser;
        try {
            newUser = userRepository.save(User.builder()
                    .fullName(cleanName).email(cleanEmail)
                    .password(passwordEncoder.encode(password)).role(cleanRole)
                    .build());
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        for (User admin : userRepository.findByRole("ADMIN")) {
            notificationService.notifyUser(
                    admin.getId(),
                    "New " + cleanRole.toLowerCase(Locale.ROOT) + " account created: " + newUser.getFullName() + ".",
                    "/admin/users"
            );
        }
        return newUser;
    }

    public User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Signed-in user was not found.");
        }
        // Reuse the authenticated user within this HTTP request. The shared navbar,
        // model advice and controller previously repeated the same MongoDB lookup.
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null) {
            Object saved = request.getAttribute("eventflow.currentUser", RequestAttributes.SCOPE_REQUEST);
            if (saved instanceof User cached && authentication.getName().equalsIgnoreCase(cached.getEmail())) {
                return cached;
            }
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Signed-in user was not found."));
        if (request != null) request.setAttribute("eventflow.currentUser", user, RequestAttributes.SCOPE_REQUEST);
        return user;
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public Optional<User> findOptional(String id) {
        return userRepository.findById(id);
    }

    public List<User> findAllOrdered() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    public List<User> findDashboardPreview() {
        return userRepository.findTop5ByOrderByFullNameAsc();
    }

    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    public long count() {
        return userRepository.count();
    }

    public long countByRole(String role) {
        return userRepository.countByRole(role);
    }

    public User changeRole(User admin, String userId, String role) {
        User user = findById(userId);
        if (admin.getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot change your own admin role.");
        }
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo account roles are fixed and cannot be changed.");
        }

        String cleanRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ALL_ROLES.contains(cleanRole)) {
            throw new IllegalArgumentException("Invalid user role.");
        }

        user.setRole(cleanRole);
        userRepository.save(user);
        notificationService.notifyUser(
                user.getId(),
                "Your EventFlow role was changed to " + cleanRole + ". Sign out and sign in again to apply the new access level.",
                "/dashboard"
        );
        return user;
    }

    public User toggleEnabled(User admin, String userId) {
        User user = findById(userId);
        if (admin.getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot disable your own account.");
        }
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo accounts cannot be disabled.");
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        notificationService.notifyUser(
                user.getId(),
                user.isEnabled()
                        ? "Your EventFlow account has been enabled by an administrator."
                        : "Your EventFlow account has been disabled by an administrator.",
                "/dashboard"
        );
        return user;
    }

    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventCount", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "registrationCount", allEntries = true, beforeInvocation = true)
    })
    public void deleteUser(User admin, String userId) {
        User user = findById(userId);
        if (admin.getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo accounts are protected and cannot be deleted.");
        }

        List<Event> ownedEvents = eventRepository.findByOrganizerIdOrderByDateAsc(user.getId());
        for (Event event : ownedEvents) {
            for (Registration registration : registrationRepository.findByEventIdOrderByRegisteredAtAsc(event.getId())) {
                notificationService.notifyUser(
                        registration.getParticipantId(),
                        event.getTitle() + " was cancelled because its organizer account was removed.",
                        "/participant/registrations"
                );
            }
            registrationRepository.deleteAllByEventId(event.getId());
            feedbackRepository.deleteAllByEventId(event.getId());
            eventProgramService.deleteAllByEvent(event.getId());
            eventTicketTypeRepository.deleteAllByEventId(event.getId());
            couponRepository.deleteAllByEventId(event.getId());
            eventGalleryService.deleteAllByEvent(event.getId());
            eventImageRepository.deleteByEventId(event.getId());
            eventRepository.delete(event);
        }

        registrationRepository.deleteAllByParticipantId(user.getId());
        feedbackRepository.deleteAllByParticipantId(user.getId());
        profileImageRepository.deleteByUserId(user.getId());
        notificationService.deleteAllForUser(user.getId());
        userRepository.delete(user);
    }

    @Caching(evict = {
            @CacheEvict(value = "eventsAll", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "featuredEvents", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "eventsByOrganizer", allEntries = true, beforeInvocation = true)
    })
    public User updateProfile(User user, ProfileUpdateRequest request) {
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo account profile information is fixed and cannot be changed.");
        }

        String cleanName = cleanName(request.fullName());
        if (!NAME_PATTERN.matcher(cleanName).matches()) {
            throw new IllegalArgumentException("Enter a valid full name (3-50 characters).");
        }

        user.setFullName(cleanName);
        userRepository.save(user);

        List<Event> events = eventRepository.findByOrganizerIdOrderByDateAsc(user.getId());
        for (Event event : events) {
            event.setOrganizerName(cleanName);
        }
        if (!events.isEmpty()) {
            eventRepository.saveAll(events);
        }

        List<Registration> registrations = registrationRepository.findByParticipantIdOrderByRegisteredAtDesc(user.getId());
        for (Registration registration : registrations) {
            registration.setParticipantName(cleanName);
        }
        if (!registrations.isEmpty()) {
            registrationRepository.saveAll(registrations);
        }

        List<Feedback> feedback = feedbackRepository.findByParticipantIdOrderByUpdatedAtDesc(user.getId());
        for (Feedback item : feedback) {
            item.setParticipantName(cleanName);
        }
        if (!feedback.isEmpty()) {
            feedbackRepository.saveAll(feedback);
        }
        return user;
    }

    public User uploadProfileImage(User user, MultipartFile image) {
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo account profile pictures are fixed.");
        }
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Choose a profile picture first.");
        }
        if (image.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new IllegalArgumentException("Profile picture must be 2 MB or smaller.");
        }

        String contentType = image.getContentType() == null
                ? ""
                : image.getContentType().toLowerCase(Locale.ROOT);
        if (!PROFILE_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Profile picture must be JPG, PNG or WEBP.");
        }

        try {
            profileImageRepository.deleteByUserId(user.getId());
            ProfileImage stored = profileImageRepository.save(ProfileImage.builder().userId(user.getId()).contentType(contentType).data(image.getBytes()).build());
            user.setProfileImageUrl("/profile-images/" + user.getId() + "?v=" + stored.getId());
            return userRepository.save(user);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not save the profile picture. Try again.");
        }
    }

    public User removeProfileImage(User user) {
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo account profile pictures are fixed.");
        }
        profileImageRepository.deleteByUserId(user.getId());
        user.setProfileImageUrl(null);
        return userRepository.save(user);
    }

    public Optional<ProfileImage> findProfileImage(String userId) {
        return profileImageRepository.findByUserId(userId);
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        if (isDemoAccount(user)) {
            throw new IllegalArgumentException("Demo account passwords are fixed and cannot be changed.");
        }

        String currentPassword = request.currentPassword() == null ? "" : request.currentPassword();
        String newPassword = request.newPassword() == null ? "" : request.newPassword();
        String confirmPassword = request.confirmPassword() == null ? "" : request.confirmPassword();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException(
                    "New password must have 8+ characters, uppercase, lowercase, number and special character."
            );
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New passwords do not match.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Choose a password different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void validatePasswordResetEmail(String email) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email."));

        if (isDemoAccount(user)) {
            throw new IllegalArgumentException(
                    "Demo account passwords cannot be reset."
            );
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("This account is disabled. Contact the EventFlow administrator.");
        }
    }

    public void resetForgottenPassword(String email, String newPassword, String confirmPassword) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        validatePasswordResetEmail(cleanEmail);

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email."));

        String password = newPassword == null ? "" : newPassword;
        String confirm = confirmPassword == null ? "" : confirmPassword;

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "New password must have 8+ characters, uppercase, lowercase, number and special character."
            );
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("New passwords do not match.");
        }
        if (passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Choose a password different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    private String cleanName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
