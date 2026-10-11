package com.eventflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Stored record of an EventFlow simulated payment. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "payment_attempts")
public class PaymentAttempt {
    @Id private String id;
    @Indexed(unique = true) private String transactionId;
    @Indexed private String eventId;
    @Indexed private String participantId;
    private String participantName;
    private String participantEmail;
    @Indexed private String ticketTypeId;
    private String ticketTypeName;
    private BigDecimal ticketPrice;
    @Indexed private String couponId;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String preferredMethod;
    @Indexed private String status;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

}
