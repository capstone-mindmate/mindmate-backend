package com.mindmate.mindmate_server.emoticon.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emoticons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Emoticon extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    private EmoticonStatus status;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean isDefault;

    // 일단 지금은 개별 이모티콘 처리
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "emoticon_pack_id")
//    private EmoticomPack emoticomPack;


    @Builder
    public Emoticon(String name, String storedName, String imageUrl, String contentType, long fileSize, int price, boolean isDefault) {
        this.name = name;
        this.storedName = storedName;
        this.imageUrl = imageUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.price = price;
        this.isDefault = isDefault;
        this.status = isDefault ? EmoticonStatus.ACCEPT : EmoticonStatus.PENDING;
    }

    public void updateStatus(EmoticonStatus status) {
        this.status = status;
    }
}
