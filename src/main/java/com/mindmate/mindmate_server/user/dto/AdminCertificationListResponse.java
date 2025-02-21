package com.mindmate.mindmate_server.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminCertificationListResponse {
    private List<AdminCertificationResponse> certifications;
    private int totalCount;
}