package com.mindmate.mindmate_server.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ImageResponse {
    private Long id;
    private String imageUrl;
}
