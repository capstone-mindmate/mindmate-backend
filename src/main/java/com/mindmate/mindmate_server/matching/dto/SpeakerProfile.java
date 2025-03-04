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
public class SpeakerProfile {
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
