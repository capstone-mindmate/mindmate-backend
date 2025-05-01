package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.service.EmoticonService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineContent;
import com.mindmate.mindmate_server.magazine.domain.MagazineContentType;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.dto.MagazineContentDTO;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineContentServiceTest {
    @Mock private EmoticonService emoticonService;
    @Mock private MagazineImageService magazineImageService;
    @Mock private MagazineContentRepository magazineContentRepository;

    @InjectMocks
    private MagazineContentService magazineContentService;

    @Mock private Magazine mockMagazine;


    // TEXT, IMAGE, EMOTICON 정상/스킵 케이스 파라미터화
    @ParameterizedTest(name = "[{index}] type={0}, text={1}, imageId={2}, emoticonId={3}, expectSave={4}")
    @MethodSource("contentScenarios")
    void processContents_ParamTest(
            MagazineContentType type,
            String text,
            Long imageId,
            Long emoticonId,
            boolean expectSave
    ) {
        // given
        MagazineContentDTO dto = MagazineContentDTO.builder()
                .type(type)
                .text(text)
                .imageId(imageId)
                .emoticonId(emoticonId)
                .build();

        if (type == MagazineContentType.IMAGE && imageId != null && expectSave) {
            MagazineImage mockImage = mock(MagazineImage.class);
            when(magazineImageService.findMagazineImageById(imageId)).thenReturn(mockImage);
            when(magazineContentRepository.findByImageAndType(mockImage, MagazineContentType.IMAGE))
                    .thenReturn(Optional.empty());
        }

        if (type == MagazineContentType.EMOTICON && emoticonId != null && expectSave) {
            Emoticon mockEmoticon = mock(Emoticon.class);
            when(emoticonService.findEmoticonById(emoticonId)).thenReturn(mockEmoticon);
        }

        // when
        magazineContentService.processContents(mockMagazine, List.of(dto));

        // then
        if (expectSave) {
            verify(magazineContentRepository).save(any(MagazineContent.class));
            verify(mockMagazine).addContent(any(MagazineContent.class));
        } else {
            verify(magazineContentRepository, never()).save(any());
            verify(mockMagazine, never()).addContent(any());
        }
    }
    static Stream<Arguments> contentScenarios() {
        return Stream.of(
                // TEXT
                Arguments.of(MagazineContentType.TEXT, "본문", null, null, true),
                Arguments.of(MagazineContentType.TEXT, "   ", null, null, false),
                // IMAGE
                Arguments.of(MagazineContentType.IMAGE, null, 10L, null, true),
                Arguments.of(MagazineContentType.IMAGE, null, null, null, false),
                // EMOTICON
                Arguments.of(MagazineContentType.EMOTICON, null, null, 20L, true),
                Arguments.of(MagazineContentType.EMOTICON, null, null, null, false)
        );
    }

    @Test
    @DisplayName("IMAGE - 이미 사용된 이미지 예외")
    void processContents_ImageType_AlreadyUsed_Throws() {
        // given
        Long imageId = 10L;
        MagazineImage mockImage = mock(MagazineImage.class);
        MagazineContent usedContent = mock(MagazineContent.class);

        MagazineContentDTO dto = MagazineContentDTO.builder()
                .type(MagazineContentType.IMAGE)
                .imageId(imageId)
                .build();

        when(magazineImageService.findMagazineImageById(imageId)).thenReturn(mockImage);
        when(magazineContentRepository.findByImageAndType(mockImage, MagazineContentType.IMAGE))
                .thenReturn(Optional.of(usedContent));
        when(usedContent.getMagazine()).thenReturn(mock(Magazine.class));

        // when & then
        assertThrows(CustomException.class, () -> magazineContentService.processContents(mockMagazine, List.of(dto)));
    }

}