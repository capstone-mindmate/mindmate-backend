package com.mindmate.mindmate_server.matching.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerProfileMatchingRequest {
    private Long id;
    private String nickname;
    private String profileImage;
    private String preferredCounselingStyle;
    private Integer counselingCount;
    private Float averageRating;
    private Set<String> requestedFields;
    private String preferredStyle;
    // speaker status를 만들지 고민중
    private boolean isWaiting;
    private LocalDateTime waitingSince;

}
