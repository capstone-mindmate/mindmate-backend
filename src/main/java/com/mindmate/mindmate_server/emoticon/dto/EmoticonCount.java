package com.mindmate.mindmate_server.emoticon.dto;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmoticonCount {
    private Emoticon emoticon;
    private Long count;
}
