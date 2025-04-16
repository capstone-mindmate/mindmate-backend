package com.mindmate.mindmate_server.point.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private PointReasonType reason;

    // 추가 설명 필요? - 누구를 리뷰 해줘서 얻었는지 이런것

    @Builder
    public PointTransaction(User user, Integer amount, TransactionType transactionType,
                            PointReasonType reason) {
        this.user = user;
        this.amount = amount;
        this.transactionType = transactionType;
        this.reason = reason;
    }
}