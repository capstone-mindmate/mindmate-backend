package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToastBoxKeywordDTO {
    private Long id;
    private String keyword;
    private String title;
    private String content;
    private String linkUrl;
    private String imageUrl;
    private boolean active;

    public static ToastBoxKeywordDTO from(ToastBoxKeyword entity) {
        return ToastBoxKeywordDTO.builder()
                .id(entity.getId())
                .keyword(entity.getKeyword())
                .title(entity.getTitle())
                .content(entity.getContent())
                .linkUrl(entity.getLinkUrl())
                .imageUrl(entity.getImageUrl())
                .active(entity.isActive())
                .build();
    }
}
