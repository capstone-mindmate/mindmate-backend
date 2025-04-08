package com.mindmate.mindmate_server.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private boolean liked;
    private int likeCount;

    public static LikeResponse of(boolean liked, int likeCount) {
        return LikeResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
