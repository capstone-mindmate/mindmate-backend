package com.mindmate.mindmate_server.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CounselingResponse {
    private String title;

    public CounselingResponse(String title) {
        this.title = title;
    }
}
