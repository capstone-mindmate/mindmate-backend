package com.mindmate.mindmate_server.emoticon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmoticonMessageRequest {
    private Long roomId;
    private Long emoticonId;
}
