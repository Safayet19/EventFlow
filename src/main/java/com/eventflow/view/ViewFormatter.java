package com.eventflow.view;

import com.eventflow.model.ContactRequest;
import com.eventflow.model.Coupon;
import com.eventflow.model.Event;
import com.eventflow.model.EventScheduleItem;
import com.eventflow.model.EventSponsor;
import com.eventflow.model.EventTicketType;
import com.eventflow.model.Feedback;
import com.eventflow.model.Notification;
import com.eventflow.model.Registration;
import com.eventflow.model.User;
import com.eventflow.service.CouponRules;
import com.eventflow.service.EventRules;
import com.eventflow.service.RegistrationRules;
import com.eventflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Read-only presentation values for Thymeleaf; MongoDB models remain data-only. */
@Component("display")
@RequiredArgsConstructor
public class ViewFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Map<String, String> REQUEST_TYPES = Map.of(
            "GENERAL_INQUIRY", "General Inquiry",
            "EVENT_SUPPORT", "Event Support",
            "REGISTRATION_ISSUE", "Registration Issue",
            "COUPON_PRICING_ISSUE", "Coupon / Pricing Issue",
            "ORGANIZER_SUPPORT", "Organizer Support",
            "TECHNICAL_PROBLEM", "Technical Problem",
            "OTHER", "Other");

    private final UserService userService;

    public String status(Event event) { return EventRules.status(event); }
    public boolean paid(Event event) { return EventRules.isPaid(event); }
    public String pricingType(Event event) { return EventRules.pricingType(event); }
    public BigDecimal basePrice(Event event) { return EventRules.basePrice(event); }
    public String eventDate(Event event) { return date(event == null ? null : event.getDate()); }
    public String eventTime(Event event) { return time(event == null ? null : event.getTime()); }
    public String formattedBasePrice(Event event) {
        BigDecimal price = EventRules.basePrice(event);
        return !EventRules.isPaid(event) || price.signum() <= 0 ? "Free" : bdt(price);
    }
    public String listingImageUrl(Event event) {
        if (event == null || event.getImageUrl() == null || event.getImageUrl().isBlank()) {
            return "/images/event-visual.png";
        }
        return event.getImageUrl();
    }

    public String ticketPrice(EventTicketType ticket) {
        return freeOrMoney(ticket == null ? null : ticket.getPrice());
    }
    public String discount(Coupon coupon) {
        BigDecimal amount = coupon == null || coupon.getDiscountValue() == null
                ? BigDecimal.ZERO : coupon.getDiscountValue();
        String number = normalizedAmount(amount);
        return coupon != null && "PERCENT".equalsIgnoreCase(coupon.getDiscountType())
                ? number + "% OFF" : "৳" + number + " OFF";
    }
    public String couponExpiry(Coupon coupon) {
        return coupon == null || coupon.getExpiryDate() == null ? "No expiry" : date(coupon.getExpiryDate());
    }
    public String couponStatus(Coupon coupon) {
        if (coupon == null || !coupon.isActive()) return "PAUSED";
        return CouponRules.expired(coupon) ? "EXPIRED" : "ACTIVE";
    }
    public String sponsorLevel(EventSponsor sponsor) {
        if (sponsor == null || sponsor.getLevel() == null || sponsor.getLevel().isBlank()) return "Partner";
        return switch (sponsor.getLevel().toUpperCase(java.util.Locale.ROOT)) {
            case "TITLE" -> "Title Sponsor";
            case "GOLD" -> "Gold Sponsor";
            case "SILVER" -> "Silver Sponsor";
            case "BRONZE" -> "Bronze Sponsor";
            default -> "Partner";
        };
    }
    public String scheduleStart(EventScheduleItem item) {
        return time(item == null ? null : item.getStartTime());
    }
    public String scheduleEnd(EventScheduleItem item) {
        return time(item == null ? null : item.getEndTime());
    }
    public String scheduleRange(EventScheduleItem item) {
        if (item == null || item.getStartTime() == null) return "TBA";
        return item.getEndTime() == null ? scheduleStart(item)
                : scheduleStart(item) + " - " + scheduleEnd(item);
    }
    public String ticketPaid(Registration registration) {
        if (registration == null) return "Free";
        BigDecimal value = registration.getFinalPrice() == null
                ? registration.getTicketPrice() : registration.getFinalPrice();
        return freeOrMoney(value);
    }
    public String originalTicketPrice(Registration registration) {
        return freeOrMoney(registration == null ? null : registration.getTicketPrice());
    }
    public String discountAmount(Registration registration) {
        BigDecimal amount = registration == null ? null : registration.getDiscountAmount();
        return amount == null || amount.signum() <= 0 ? "৳0" : bdt(amount);
    }
    public boolean paidTicket(Registration registration) {
        return RegistrationRules.paidTicket(registration);
    }
    public boolean hasPaymentReceipt(Registration registration) {
        return paidTicket(registration) && registration.getPaymentStatus() != null
                && registration.getPaymentStatus().startsWith("PAID");
    }
    public boolean discounted(Registration registration) {
        return RegistrationRules.discounted(registration);
    }
    public String ticketName(Registration registration) {
        String name = registration == null ? null : registration.getTicketTypeName();
        return name == null || name.isBlank() ? "General Admission" : name;
    }
    public String registrationTime(Registration registration) {
        return dateTime(registration == null ? null : registration.getRegisteredAt());
    }

    public String requestType(ContactRequest request) {
        return REQUEST_TYPES.getOrDefault(request == null || request.getRequestType() == null
                ? "" : request.getRequestType(), "Other");
    }
    public String requestStatus(ContactRequest request) {
        String status = request == null || request.getStatus() == null ? "NEW" : request.getStatus();
        return switch (status) {
            case "IN_PROGRESS" -> "In Progress";
            case "RESOLVED" -> "Resolved";
            default -> "New";
        };
    }
    public String requestCreated(ContactRequest request) {
        return dateTime(request == null ? null : request.getCreatedAt());
    }
    public String requestUpdated(ContactRequest request) {
        return dateTime(request == null ? null : request.getUpdatedAt());
    }
    public String feedbackUpdated(Feedback feedback) {
        return dateTime(feedback == null ? null : feedback.getUpdatedAt());
    }
    public String notificationTime(Notification notice) {
        return dateTime(notice == null ? null : notice.getCreatedAt());
    }
    public String formattedTicketValue(BigDecimal value) {
        return value == null || value.signum() <= 0 ? "৳0" : bdt(value);
    }
    public boolean demoAccount(User user) { return userService.isDemoAccount(user); }

    private String date(LocalDate value) { return value == null ? "TBA" : value.format(DATE); }
    private String time(LocalTime value) { return value == null ? "TBA" : value.format(TIME); }
    private String dateTime(LocalDateTime value) { return value == null ? "—" : value.format(DATE_TIME); }
    private String freeOrMoney(BigDecimal value) {
        return value == null || value.signum() <= 0 ? "Free" : bdt(value);
    }
    private String bdt(BigDecimal value) { return "৳" + normalizedAmount(value); }
    private String normalizedAmount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
