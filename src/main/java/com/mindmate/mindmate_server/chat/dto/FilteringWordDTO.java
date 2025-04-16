package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FilteringWordDTO {
    private Long id;
    private String word;
    private boolean active;
    private LocalDateTime createdAt;

    public static FilteringWordDTO from(FilteringWord word) {
        return FilteringWordDTO.builder()
                .id(word.getId())
                .word(word.getWord())
                .active(word.isActive())
                .createdAt(word.getCreatedAt())
                .build();
    }
}
