package com.mindmate.mindmate_server.point.domain;

import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointBalance {
    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Integer balance;

    @Version  // 동시성 제어..
    private Long version;

    public PointBalance(User user) {
        this.user = user;
        this.balance = 0;
    }

    public void addPoints(int amount) {
        this.balance += amount;
    }

    public boolean usePoints(int amount) {
        if (this.balance < amount) {
            return false;
        }
        this.balance -= amount;
        return true;
    }
}