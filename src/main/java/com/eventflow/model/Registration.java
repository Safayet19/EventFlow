package com.eventflow.model;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "registrations")
@CompoundIndexes({
        @CompoundIndex(name = "event_participant_unique", def = "{'eventId': 1, 'participantId': 1}", unique = true),
        @CompoundIndex(name = "registration_event_registered", def = "{'eventId': 1, 'registeredAt': -1}"),
        @CompoundIndex(name = "registration_participant_registered", def = "{'participantId': 1, 'registeredAt': -1}")
})
public class Registration {

    @Id
    private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String participantId;

    private String participantName;
    private String participantEmail;
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();
    @Indexed
    private String ticketTypeId;
    private String ticketTypeName;
    @Builder.Default
    private BigDecimal ticketPrice = BigDecimal.ZERO;
    @Indexed
    private String couponId;
    private String couponCode;
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal finalPrice;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;

}
