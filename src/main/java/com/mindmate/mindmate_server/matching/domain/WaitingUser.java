package com.mindmate.mindmate_server.matching.domain;

import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "waiting_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id", nullable = false)
    private Matching matching;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(length = 100)
    private String message;

    // status?
    @Builder
    public WaitingUser(User applicant, String message) {
        this.applicant = applicant;
        this.message = message;
    }


}
