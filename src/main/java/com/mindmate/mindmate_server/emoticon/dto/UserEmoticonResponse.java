package com.mindmate.mindmate_server.emoticon.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserEmoticonResponse {
    private List<EmoticonResponse> ownedEmoticons;
    private List<EmoticonResponse> notOwnedEmoticons;
}
