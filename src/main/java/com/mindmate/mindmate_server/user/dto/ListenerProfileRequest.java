package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.ListenerCounselingField;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class ListenerProfileRequest {
    private String nickname;
    private String profileImage;
    private Set<CounselingField> counselingFields;
    private CounselingStyle counselingStyle;
    private LocalDateTime availableTime;
}
