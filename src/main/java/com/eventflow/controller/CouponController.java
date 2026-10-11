package com.eventflow.controller;

import com.eventflow.dto.CouponForm;
import com.eventflow.model.Coupon;
import com.eventflow.model.Event;
import com.eventflow.model.User;
import com.eventflow.service.CouponService;
import com.eventflow.service.EventRules;
import com.eventflow.service.EventService;
import com.eventflow.service.TicketTypeService;
import com.eventflow.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CouponController {

    private final UserService userService;
    private final EventService eventService;
    private final CouponService couponService;
    private final TicketTypeService ticketTypeService;



    @GetMapping("/organizer/coupons")
    public String organizerCoupons(Authentication authentication, Model model) {
        User organizer = userService.currentUser(authentication);
        List<Event> events = eventService.findByOrganizer(organizer.getId());
        List<Coupon> coupons = couponService.findByOrganizer(organizer.getId());
        populateModel(model, events, coupons, "organizer");
        return "coupons";
    }

    @GetMapping("/admin/coupons")
    public String adminCoupons(Model model) {
        List<Event> events = eventService.findAllSorted();
        List<Coupon> coupons = couponService.findAll();
        populateModel(model, events, coupons, "admin");
        return "coupons";
    }

    @PostMapping("/organizer/coupons")
    public String addOrganizerCoupon(@ModelAttribute CouponForm form,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        return addCoupon(form, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/coupons")
    public String addAdminCoupon(@ModelAttribute CouponForm form,
                                 RedirectAttributes redirectAttributes) {
        return addCoupon(form, null, true, redirectAttributes);
    }

    @PostMapping("/organizer/coupons/{couponId}/edit")
    public String editOrganizerCoupon(@PathVariable String couponId,
                                      @ModelAttribute CouponForm form,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        return editCoupon(couponId, form, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/coupons/{couponId}/edit")
    public String editAdminCoupon(@PathVariable String couponId,
                                  @ModelAttribute CouponForm form,
                                  RedirectAttributes redirectAttributes) {
        return editCoupon(couponId, form, null, true, redirectAttributes);
    }

    @PostMapping("/organizer/coupons/{couponId}/delete")
    public String deleteOrganizerCoupon(@PathVariable String couponId,
                                        @RequestParam String eventId,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        return deleteCoupon(couponId, eventId, authentication, false, redirectAttributes);
    }

    @PostMapping("/admin/coupons/{couponId}/delete")
    public String deleteAdminCoupon(@PathVariable String couponId,
                                    @RequestParam String eventId,
                                    RedirectAttributes redirectAttributes) {
        return deleteCoupon(couponId, eventId, null, true, redirectAttributes);
    }

    @GetMapping("/participant/events/{eventId}/coupon-preview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewCoupon(@PathVariable String eventId,
                                                              @RequestParam(required = false) String ticketTypeId,
                                                              @RequestParam String couponCode) {
        try {
            Event event = eventService.findById(eventId);
            TicketTypeService.TicketSelection ticket = ticketTypeService.resolveForRegistration(event, ticketTypeId);
            CouponService.AppliedCoupon applied = couponService.apply(event, ticket.price(), couponCode);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("couponCode", applied.couponCode());
            result.put("originalPrice", money(ticket.price()));
            result.put("discount", money(applied.discountAmount()));
            result.put("finalPrice", money(applied.finalPrice()));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("valid", false, "message", ex.getMessage()));
        }
    }

    private String addCoupon(CouponForm form,
                             Authentication authentication,
                             boolean admin,
                             RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(form.eventId(), authentication, admin);
            couponService.create(event, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Coupon created successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectCoupons(admin);
    }

    private String editCoupon(String couponId,
                              CouponForm form,
                              Authentication authentication,
                              boolean admin,
                              RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(form.eventId(), authentication, admin);
            couponService.update(event, couponId, form);
            redirectAttributes.addFlashAttribute("toastSuccess", "Coupon updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectCoupons(admin);
    }

    private String deleteCoupon(String couponId,
                                String eventId,
                                Authentication authentication,
                                boolean admin,
                                RedirectAttributes redirectAttributes) {
        try {
            Event event = managedEvent(eventId, authentication, admin);
            couponService.delete(event, couponId);
            redirectAttributes.addFlashAttribute("toastSuccess", "Coupon deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toastError", ex.getMessage());
        }
        return redirectCoupons(admin);
    }

    private Event managedEvent(String eventId, Authentication authentication, boolean admin) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Choose an event for the coupon.");
        }
        if (admin) return eventService.findById(eventId);
        User organizer = userService.currentUser(authentication);
        return eventService.findOwned(eventId, organizer);
    }

    private void populateModel(Model model, List<Event> events, List<Coupon> coupons, String mode) {
        Map<String, Event> eventMap = new HashMap<>();
        for (Event event : events) eventMap.put(event.getId(), event);

        model.addAttribute("events", events);
        model.addAttribute("paidEvents", events.stream().filter(EventRules::isPaid).filter(eventService::isUpcoming).toList());
        model.addAttribute("coupons", coupons);
        model.addAttribute("eventMap", eventMap);
        Map<String, Long> usageCounts = couponService.usageCounts(coupons);
        model.addAttribute("usageCounts", usageCounts);
        model.addAttribute("couponRedemptions", usageCounts.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("activeCouponCount", couponService.activeCount(coupons));
        model.addAttribute("paidEventCount", events.stream().filter(EventRules::isPaid).filter(eventService::isUpcoming).count());
        model.addAttribute("couponMode", mode);
        model.addAttribute("couponBase", "/" + mode + "/coupons");
    }

    private String redirectCoupons(boolean admin) {
        return "redirect:/" + (admin ? "admin" : "organizer") + "/coupons";
    }

    private String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.compareTo(BigDecimal.ZERO) <= 0) return "Free";
        return "৳" + value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
