package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.service.EmoticonInteractionService;
import com.mindmate.mindmate_server.emoticon.service.EmoticonService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineContent;
import com.mindmate.mindmate_server.magazine.domain.MagazineContentType;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.dto.MagazineContentDTO;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MagazineContentService {
    private final EmoticonService emoticonService;
    private final MagazineImageService magazineImageService;
    private final EmoticonInteractionService emoticonInteractionService;

    private final MagazineContentRepository magazineContentRepository;

    public void processContents(Magazine magazine, List<MagazineContentDTO> contents) {
        int order = 0;
        for (MagazineContentDTO dto : contents) {
            MagazineContent content = createContent(magazine, dto, order);
            if (content != null) {
                content.setContentOrder(order++);
                magazine.addContent(content);
                magazineContentRepository.save(content);
            }
        }
    }

    private MagazineContent createContent(Magazine magazine, MagazineContentDTO dto, int order) {
        switch (dto.getType()) {
            case TEXT:
                if (StringUtils.isBlank(dto.getText())) return null;
                return MagazineContent.builder()
                        .magazine(magazine)
                        .type(MagazineContentType.TEXT)
                        .text(dto.getText())
                        .build();

            case IMAGE:
                if (dto.getImageId() == null) return null;
                MagazineImage image = magazineImageService.findMagazineImageById(dto.getImageId());
                Optional<MagazineContent> usedContent = magazineContentRepository
                        .findByImageAndType(image, MagazineContentType.IMAGE);
                if (usedContent.isPresent() && !usedContent.get().getMagazine().equals(magazine)) {
                    throw new CustomException(MagazineErrorCode.MAGAZINE_IMAGE_ALREADY_IN_USE);
                }
                return MagazineContent.builder()
                        .magazine(magazine)
                        .type(MagazineContentType.IMAGE)
                        .image(image)
                        .build();

            case EMOTICON:
                if (dto.getEmoticonId() == null) return null;
                Emoticon emoticon = emoticonService.findEmoticonById(dto.getEmoticonId());
                emoticonInteractionService.incrementUsage(emoticon.getId());

                return MagazineContent.builder()
                        .magazine(magazine)
                        .type(MagazineContentType.EMOTICON)
                        .emoticon(emoticon)
                        .build();

            default:
                return null;
        }
    }
}
