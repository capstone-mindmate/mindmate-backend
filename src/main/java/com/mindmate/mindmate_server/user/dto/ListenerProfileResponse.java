package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ListenerProfileResponse extends BaseProfileResponse {
    private List<CounselingField> counselingFields;
    private CounselingStyle counselingStyle;
    private Integer avgResponseTime;
    private String availableTimes;
    private String badgeStatus;
}

