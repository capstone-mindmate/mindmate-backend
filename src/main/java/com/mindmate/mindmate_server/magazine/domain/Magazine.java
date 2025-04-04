package com.mindmate.mindmate_server.magazine.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "magazines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Magazine extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineLike> likes = new ArrayList<>();

    private int likeCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MagazineStatus magazineStatus;

    @Enumerated(EnumType.STRING)
    private MatchingCategory category;

    @Builder
    public Magazine(String title, String content, User author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public void update(String title, String content, MatchingCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void setCategory(MatchingCategory category) {
        this.category = category;
    }

    public void setStatus(MagazineStatus status) {
        this.magazineStatus = status;
    }

    // todo: 좋아요 관리, 사진 관리

}
