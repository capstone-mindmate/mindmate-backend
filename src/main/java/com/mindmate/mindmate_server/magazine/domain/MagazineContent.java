package com.mindmate.mindmate_server.magazine.domain;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "magazine_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MagazineContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MagazineContentType type;

    @Column(columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private MagazineImage image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emoticon_id")
    private Emoticon emoticon;

    @Column(nullable = false)
    private int contentOrder;

    @Builder
    public MagazineContent(Magazine magazine, MagazineContentType type, String text, MagazineImage image, Emoticon emoticon, int contentOrder) {
        this.magazine = magazine;
        this.type = type;
        this.text = text;
        this.image = image;
        this.emoticon = emoticon;
        this.contentOrder = contentOrder;
    }

    public void setMagazine(Magazine magazine) {
        this.magazine = magazine;
    }
}
