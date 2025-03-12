package com.mindmate.mindmate_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSimpleResponse {
    // todo : 어떤 걸 보여주면 좋을지 얘기 해봐야할듯
    private Long id;
    private Long userId;
    private String nickname;
    private String profileImage;
    private int totalCounselingCount;
    private Double averageRating;
}