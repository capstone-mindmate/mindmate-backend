package com.mindmate.mindmate_server.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFilteringHistoryDTO {
    private Long userId;
    private String email;
    private String nickname;
    private Map<Long, Integer> roomFilterCounts;
    private List<FilteredContentDTO> recentFilteredContents;
    private int totalFilterCount;
    private Date lastFilteringTime;
}
