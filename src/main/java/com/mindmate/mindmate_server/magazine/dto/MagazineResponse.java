package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MagazineResponse {
    private Long id;
    private String title;
    private String content;
    private MatchingCategory category;
    private String authorName;
    private Long authorId;
    private int likeCount;
    // todo: 이미지
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MagazineResponse from(Magazine magazine) {
        return MagazineResponse.builder()
                .id(magazine.getId())
                .title(magazine.getTitle())
                .content(magazine.getContent())
                .category(magazine.getCategory())
                .authorName(magazine.getAuthor().getProfile().getNickname())
                .authorId(magazine.getAuthor().getId())
                .likeCount(magazine.getLikeCount())
                .createdAt(magazine.getCreatedAt())
                .updatedAt(magazine.getModifiedAt())
                .build();
    }
}
