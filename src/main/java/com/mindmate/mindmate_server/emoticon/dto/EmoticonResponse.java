package com.mindmate.mindmate_server.emoticon.dto;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmoticonResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private int price;
    private boolean isDefault;
    private boolean isPurchased;

    public static EmoticonResponse from(Emoticon emoticon, boolean isPurchased) {
        return EmoticonResponse.builder()
                .id(emoticon.getId())
                .name(emoticon.getName())
                .imageUrl(emoticon.getImageUrl())
                .price(emoticon.getPrice())
                .isDefault(emoticon.isDefault())
                .isPurchased(isPurchased)
                .build();
    }

    public static EmoticonResponse from(Emoticon emoticon) {
        return from(emoticon, false);
    }
}
