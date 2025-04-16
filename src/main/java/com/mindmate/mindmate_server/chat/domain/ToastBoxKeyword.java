package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "toast_box_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ToastBoxKeyword extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keyword;

    private String title;
    private String content;
    private String linkUrl;
    private String imageUrl;

    private boolean active = true;

    @Builder
    public ToastBoxKeyword(String keyword, String title, String content, String linkUrl, String imageUrl, boolean active) {
        this.keyword = keyword;
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
        this.imageUrl = imageUrl;
    }
}
