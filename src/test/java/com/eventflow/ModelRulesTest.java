package com.eventflow;

import com.eventflow.dto.ContactRequestForm;
import com.eventflow.dto.CouponForm;
import com.eventflow.dto.DashboardAnalytics;
import com.eventflow.dto.EventForm;
import com.eventflow.dto.RegisterRequest;
import com.eventflow.model.ContactRequest;
import com.eventflow.model.Coupon;
import com.eventflow.model.Event;
import com.eventflow.model.Notification;
import com.eventflow.model.Registration;
import com.eventflow.service.CouponRules;
import com.eventflow.service.EventRules;
import com.eventflow.service.RegistrationRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests: no MongoDB, Spring context, network or mocking required. */
class ModelRulesTest {

    @Test
    void requestAndResponseDtosAreRecords() {
        assertTrue(RegisterRequest.class.isRecord());
        assertTrue(ContactRequestForm.class.isRecord());
        assertTrue(EventForm.class.isRecord());
        assertTrue(CouponForm.class.isRecord());
        assertTrue(DashboardAnalytics.class.isRecord());
    }

    @Test
    void modelBuildersPreserveDefaults() {
        assertTrue(ContactRequest.builder().build().getStatus().equals("NEW"));
        assertTrue(Coupon.builder().build().isActive());
        assertTrue(Notification.builder().build().getCreatedAt() != null);
        assertEquals(BigDecimal.ZERO, Registration.builder().build().getTicketPrice());
    }

    @Test
    void eventAndCouponRulesArePreserved() {
        Event past = Event.builder().date(LocalDate.now().minusDays(1))
                .status("UPCOMING").pricingType("PAID").basePrice(new BigDecimal("500"))
                .build();
        Event future = Event.builder().date(LocalDate.now().plusDays(1)).status("UPCOMING").build();
        assertEquals("COMPLETED", EventRules.status(past));
        assertEquals("UPCOMING", EventRules.status(future));
        assertTrue(EventRules.isPaid(past));
        assertFalse(EventRules.isPaid(future));
        assertEquals(BigDecimal.ZERO, EventRules.basePrice(future));
        assertTrue(CouponRules.expired(Coupon.builder().expiryDate(LocalDate.now().minusDays(1)).build()));
        assertFalse(CouponRules.expired(Coupon.builder().expiryDate(LocalDate.now().plusDays(1)).build()));
    }

    @Test
    void paidTicketCannotBecomeFreeThroughDiscount() {
        assertFalse(RegistrationRules.paidTicket(Registration.builder()
                .ticketPrice(BigDecimal.ZERO).finalPrice(BigDecimal.ZERO)
                .paymentStatus("NOT_REQUIRED").build()));
        assertTrue(RegistrationRules.paidTicket(Registration.builder()
                .ticketPrice(new BigDecimal("1000")).finalPrice(BigDecimal.ZERO)
                .discountAmount(new BigDecimal("1000")).paymentStatus("PAID_DEMO").build()));
    }

    @Test
    void downloadableDocumentsHaveValidPdfHeaders() throws java.io.IOException {
        var registration = Registration.builder().id("ticket-demo")
                .participantName("Student").ticketTypeName("Standard")
                .ticketPrice(new BigDecimal("800")).finalPrice(new BigDecimal("800"))
                .paymentReference("EF1234567890").paymentMethod("BKASH").build();
        var event = Event.builder().title("Technology Conference")
                .location("Dhaka").date(LocalDate.now().plusDays(2)).build();
        var documents = new com.eventflow.service.RegistrationDocumentService();
        byte[] ticket = documents.ticket(registration, event);
        byte[] receipt = documents.receipt(registration, event);
        assertTrue(ticket.length > 10000);
        assertTrue(receipt.length > 10000);
        assertTrue(new String(ticket, 0, 8, java.nio.charset.StandardCharsets.US_ASCII)
                .startsWith("%PDF-1.4"));
        assertTrue(new String(receipt, 0, 8, java.nio.charset.StandardCharsets.US_ASCII)
                .startsWith("%PDF-1.4"));
    }

    @Test
    void registrationDiscountRequiresPositiveDiscountAndCoupon() {
        assertTrue(RegistrationRules.discounted(Registration.builder()
                .couponCode("SAVE100").discountAmount(new BigDecimal("100")).build()));
        assertFalse(RegistrationRules.discounted(Registration.builder()
                .couponCode(null).discountAmount(new BigDecimal("100")).build()));
        assertFalse(RegistrationRules.discounted(Registration.builder()
                .couponCode("SAVE100").discountAmount(BigDecimal.ZERO).build()));
    }
}
