package com.mindmate.mindmate_server.emoticon.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmoticonUploadRequest {
    private String name;
    private int price;
}
