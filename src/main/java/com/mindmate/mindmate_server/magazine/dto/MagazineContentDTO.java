package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.magazine.domain.MagazineContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MagazineContentDTO {
    private MagazineContentType type;
    private String text;
    private Long imageId;
    private Long emoticonId;
}
