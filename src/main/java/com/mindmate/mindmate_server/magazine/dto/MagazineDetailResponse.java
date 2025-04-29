package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MagazineDetailResponse {
    private Long id;
    private String title;
    private List<MagazineContentResponse> contents;
    private String authorName;
    private Long authorId;
    private int likeCount;
    private MagazineStatus status;
    private MatchingCategory category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean isAuthor;
    private boolean isLiked;

    public static MagazineDetailResponse from(Magazine magazine, boolean isAuthor, boolean isLiked) {
        List<MagazineContentResponse> contentResponses = magazine.getContents().stream()
                .map(MagazineContentResponse::from)
                .collect(Collectors.toList());

        return MagazineDetailResponse.builder()
                .id(magazine.getId())
                .title(magazine.getTitle())
                .contents(contentResponses)
                .authorName(magazine.getAuthor().getProfile().getNickname())
                .authorId(magazine.getAuthor().getId())
                .likeCount(magazine.getLikeCount())
                .status(magazine.getMagazineStatus())
                .category(magazine.getCategory())
                .createdAt(magazine.getCreatedAt())
                .updatedAt(magazine.getModifiedAt())
                .isAuthor(isAuthor)
                .isLiked(isLiked)
                .build();
    }
}