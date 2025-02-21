package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpeakerProfileResponse extends BaseProfileResponse {
    private CounselingStyle preferredStyle;
}
