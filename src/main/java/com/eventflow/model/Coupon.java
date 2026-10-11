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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "event_coupons")
@CompoundIndexes({
        @CompoundIndex(name = "event_coupon_code_unique", def = "{'eventId': 1, 'code': 1}", unique = true)
})
public class Coupon {

    @Id
    private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String organizerId;

    private String code;
    private String discountType;
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;
    private LocalDate expiryDate;
    private int usageLimit;
    @Builder.Default
    private boolean active = true;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

}
