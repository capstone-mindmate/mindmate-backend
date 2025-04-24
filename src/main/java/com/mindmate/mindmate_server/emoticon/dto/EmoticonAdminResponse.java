package com.mindmate.mindmate_server.emoticon.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmoticonAdminResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private int price;
    private String contentType;
    private long fileSize;
    private LocalDateTime createdAt;

}
