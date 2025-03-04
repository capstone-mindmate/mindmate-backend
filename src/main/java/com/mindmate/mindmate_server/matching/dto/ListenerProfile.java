package com.mindmate.mindmate_server.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListenerProfile {
    private Long id;
    private String nickname;
    private String profileImage;
    private String counselingStyle;
    private Set<String> counselingFields;
    private Integer counselingCount;
    private Float averageRating;
    private ListenerStatus status;
}
