// PaymentProduct.java
package com.mindmate.mindmate_server.payment.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentProduct extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Boolean isPromotion = false;

    @Column
    private String promotionPeriod;

    @Column(nullable = false)
    private Boolean active = true;

    @Builder
    public PaymentProduct( Integer points, Integer amount,
                          Boolean isPromotion, String promotionPeriod) {
        this.points = points;
        this.amount = amount;
        this.isPromotion = isPromotion != null ? isPromotion : false;
        this.promotionPeriod = promotionPeriod;
    }
}