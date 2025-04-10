package com.mindmate.mindmate_server.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementNotificationRequest {
    @NotBlank(message = "공지사항 제목은 필수입니다.")
    private String title;

    @NotNull(message = "공지사항 ID는 필수입니다.")
    private Long announcementId;
}
