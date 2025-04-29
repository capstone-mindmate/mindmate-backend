package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.magazine.domain.MagazineContent;
import com.mindmate.mindmate_server.magazine.domain.MagazineContentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MagazineContentResponse {
    private Long id;
    private MagazineContentType type;
    private String text;
    private String imageUrl;
    private String emoticonUrl;
    private String emoticonName;
    private int contentOrder;

    public static MagazineContentResponse from(MagazineContent content) {
        MagazineContentResponseBuilder builder = MagazineContentResponse.builder()
                .id(content.getId())
                .type(content.getType())
                .contentOrder(content.getContentOrder());

        switch (content.getType()) {
            case TEXT:
                builder.text(content.getText());
                break;
            case IMAGE:
                if (content.getImage() != null) {
                    builder.imageUrl(content.getImage().getImageUrl());
                }
            case EMOTICON:
                if (content.getEmoticon() != null) {
                    builder.emoticonUrl(content.getEmoticon().getImageUrl());
                    builder.emoticonName(content.getEmoticon().getName());
                }
                break;
        }
        return builder.build();
    }
}
