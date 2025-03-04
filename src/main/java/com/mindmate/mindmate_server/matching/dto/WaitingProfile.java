package com.mindmate.mindmate_server.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingProfile {
    private Long profileId;
    private String nickname;
    private String profileImage;
    private Set<String> requestedFields;
    private String preferredStyle;
    private LocalDateTime waitingSince;
    private String userType;
}