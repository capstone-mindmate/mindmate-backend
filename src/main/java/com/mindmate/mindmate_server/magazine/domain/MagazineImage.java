package com.mindmate.mindmate_server.magazine.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "magazine_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MagazineImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;

    // todo: 추가 필요 내용 + method, builder

    @Builder
    public MagazineImage(String originalName, String storedName, String imageUrl, String contentType, long fileSize, Magazine magazine) {
        this.originalName = originalName;
        this.storedName = storedName;
        this.imageUrl = imageUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.magazine = magazine;
    }

    public void setMagazine(Magazine magazine) {
        this.magazine = magazine;
    }
}
