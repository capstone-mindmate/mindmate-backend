package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.CustomFormItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFormItemDTO {
    private Long id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;

    public static CustomFormItemDTO from(CustomFormItem item) {
        return CustomFormItemDTO.builder()
                .id(item.getId())
                .question(item.getQuestion())
                .answer(item.getAnswer())
                .createdAt(item.getCreatedAt())
                .build();
    }
}

