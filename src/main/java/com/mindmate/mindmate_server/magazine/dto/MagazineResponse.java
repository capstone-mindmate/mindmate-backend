package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.*;
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
    private String content;
    private MatchingCategory category;
    private String authorName;
    private Long authorId;
    private int likeCount;
    private List<ImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MagazineResponse from(Magazine magazine) {
        List<ImageResponse> imageResponses = magazine.getImages().stream()
                .map(image -> ImageResponse.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return MagazineResponse.builder()
                .id(magazine.getId())
                .title(magazine.getTitle())
                .content(magazine.getContent())
                .category(magazine.getCategory())
                .authorName(magazine.getAuthor().getProfile().getNickname())
                .authorId(magazine.getAuthor().getId())
                .likeCount(magazine.getLikeCount())
                .images(imageResponses)
                .createdAt(magazine.getCreatedAt())
                .updatedAt(magazine.getModifiedAt())
                .build();
    }
}
