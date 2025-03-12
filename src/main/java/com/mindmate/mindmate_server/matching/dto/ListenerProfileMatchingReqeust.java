package com.mindmate.mindmate_server.matching.dto;

import lombok.*;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListenerProfileMatchingReqeust {
    private Long id;
    private String nickname;
    private String profileImage;
    private String counselingStyle;
    private Set<String> counselingFields;
    private Integer counselingCount;
    private Float averageRating;
    private ListenerStatus status;
}
