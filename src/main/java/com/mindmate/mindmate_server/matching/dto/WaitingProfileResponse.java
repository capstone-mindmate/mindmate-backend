package com.mindmate.mindmate_server.matching.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

//@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingProfileResponse {
    private Long profileId;
    private String nickname;
    private String profileImage;
    private Set<String> requestedFields;
    private String preferredStyle;
    private LocalDateTime waitingSince;
    private String userType;
}