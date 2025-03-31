package com.mindmate.mindmate_server.chat.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.mindmate.mindmate_server.chat.domain.CustomForm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFormResponse {
    private Long id;
    private Long chatRoomId;
    private Long creatorId;
    private String creatorName;
    private Long responderId;
    private String responderName;
    private boolean isAnswered;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private List<CustomFormItemDTO> items;

    public static CustomFormResponse from(CustomForm form) {
        String creatorName = form.getCreator() != null && form.getCreator().getProfile() != null ?
                form.getCreator().getProfile().getNickname() : "Unknown";

        String responderName = form.getResponder() != null && form.getResponder().getProfile() != null ?
                form.getResponder().getProfile().getNickname() : null;

        return CustomFormResponse.builder()
                .id(form.getId())
                .chatRoomId(form.getChatRoom().getId())
                .creatorId(form.getCreator() != null ? form.getCreator().getId() : null)
                .creatorName(creatorName)
                .responderId(form.getResponder() != null ? form.getResponder().getId() : null)
                .responderName(responderName)
                .isAnswered(form.isAnswered())
                .createdAt(form.getCreatedAt())
                .respondedAt(form.getAnsweredAt())
                .items(form.getItems() != null ? form.getItems().stream()
                        .map(CustomFormItemDTO::from)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }
}
