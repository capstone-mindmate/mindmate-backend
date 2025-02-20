package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ListenerProfileUpdateRequest {
    private String nickname;
    private String profileImage;
    private CounselingStyle counselingStyle;
    private List<CounselingField> counselingFields;
    private String availableTimes;
}
