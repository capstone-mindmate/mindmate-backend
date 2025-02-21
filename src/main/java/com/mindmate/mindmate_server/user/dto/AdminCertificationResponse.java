package com.mindmate.mindmate_server.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCertificationResponse {
    private Long listenerId;
    private String nickname;
    private String certificationUrl;
    private String careerDescription;
    private LocalDateTime createdAt;
}
