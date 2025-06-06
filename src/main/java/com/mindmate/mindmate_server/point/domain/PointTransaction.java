package com.mindmate.mindmate_server.point.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_version",
                        columnNames = {"user_id", "version"})
        })
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
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointReasonType reasonType;

    private Long entityId;

    @Column(nullable = false)
    private Integer balance;

    @Builder
    public PointTransaction(User user, TransactionType transactionType,
                            Integer amount, PointReasonType reasonType, Long entityId,
                            Integer balance) {
        this.user = user;
        this.transactionType = transactionType;
        this.amount = amount;
        this.reasonType = reasonType;
        this.entityId = entityId;
        this.balance = balance;
    }

    public void incrementVersion(Long baseVersion) {
        if (baseVersion == null) {
            this.version = 1L;
        } else {
            this.version = baseVersion + 1;
        }
    }
}