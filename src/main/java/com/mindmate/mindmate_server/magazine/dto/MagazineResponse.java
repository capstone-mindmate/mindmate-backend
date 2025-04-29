package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MagazineResponse {
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

    public static MagazineResponse from(Magazine magazine) {
        List<MagazineContentResponse> contentResponses = magazine.getContents().stream()
                .map(MagazineContentResponse::from)
                .collect(Collectors.toList());

        return MagazineResponse.builder()
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
                .build();
    }
}
