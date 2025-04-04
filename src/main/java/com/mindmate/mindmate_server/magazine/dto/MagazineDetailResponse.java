package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MagazineDetailResponse {
    private Long id;
    private String title;
    private String content;
    private MatchingCategory category;
    private String authorName;
    private Long authorId;
    private int likeCount;
    private boolean isAuthor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MagazineDetailResponse from(Magazine magazine, boolean isAuthor) {
        return MagazineDetailResponse.builder()
                .id(magazine.getId())
                .title(magazine.getTitle())
                .content(magazine.getContent())
                .category(magazine.getCategory())
                .authorName(magazine.getAuthor().getProfile().getNickname())
                .authorId(magazine.getAuthor().getId())
                .likeCount(magazine.getLikeCount())
                .isAuthor(isAuthor)
                .createdAt(magazine.getCreatedAt())
                .updatedAt(magazine.getModifiedAt())
                .build();
    }
}