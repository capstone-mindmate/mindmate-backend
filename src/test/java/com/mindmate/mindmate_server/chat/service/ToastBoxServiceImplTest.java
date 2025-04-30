package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordDTO;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordRequest;
import com.mindmate.mindmate_server.chat.repository.ToastBoxRepository;
import com.mindmate.mindmate_server.chat.util.ToastBoxAdapter;
import com.mindmate.mindmate_server.global.exception.CustomException;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToastBoxServiceImplTest {
    @Mock private ToastBoxRepository toastBoxRepository;
    @Mock private ToastBoxAdapter toastBoxAdapter;

    @InjectMocks
    private ToastBoxServiceImpl toastBoxService;

    private ToastBoxKeyword createKeyword(Long id, String keyword, String title, boolean active) {
        ToastBoxKeyword toastBoxKeyword = mock(ToastBoxKeyword.class);
        when(toastBoxKeyword.getId()).thenReturn(id);
        when(toastBoxKeyword.getKeyword()).thenReturn(keyword);
        when(toastBoxKeyword.getTitle()).thenReturn(title);
        when(toastBoxKeyword.isActive()).thenReturn(active);
        return toastBoxKeyword;
    }

    @Test
    @DisplayName("초기화 시 활성화된 키워드만 어댑터에 전달")
    void initialize_ShouldLoadActiveKeywords() {
        // given
        List<ToastBoxKeyword> activeKeywords = List.of(
                createKeyword(1L, "keyword1", "title1", true),
                createKeyword(2L, "keyword2", "title2", true)
        );
        when(toastBoxRepository.findByActiveTrue()).thenReturn(activeKeywords);

        // when
        toastBoxService.refreshToastBoxKeywords();

        // then
        verify(toastBoxRepository).findByActiveTrue();
        verify(toastBoxAdapter).initialize(activeKeywords);
    }

    @ParameterizedTest
    @DisplayName("컨텐츠 검색 시 매칭되는 키워드 반환")
    @MethodSource("findKeywordScenarios")
    void findToastBoxKeywords_Scenarios(String content, List<ToastBoxKeyword> expectedKeywords) {
        // given
        when(toastBoxAdapter.findMatchingKeywords(content)).thenReturn(expectedKeywords);

        // when
        List<ToastBoxKeyword> result = toastBoxService.findToastBoxKeywords(content);

        // then
        assertThat(result).isEqualTo(expectedKeywords);
        if (content != null && !content.trim().isEmpty()) {
            verify(toastBoxAdapter).findMatchingKeywords(content);
        }
    }

    static Stream<Arguments> findKeywordScenarios() {
        ToastBoxKeyword keyword1 = mock(ToastBoxKeyword.class);
        ToastBoxKeyword keyword2 = mock(ToastBoxKeyword.class);
        List<ToastBoxKeyword> keywords = List.of(keyword1, keyword2);

        return Stream.of(
                Arguments.of("유효한 컨텐츠", keywords),
                Arguments.of("", Collections.emptyList()),
                Arguments.of(null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("모든 토스트박스 키워드 조회")
    void getAllToastBoxWords_ShouldReturnAllKeywords() {
        // given
        List<ToastBoxKeyword> keywords = List.of(
                createKeyword(1L, "keyword1", "title1", true),
                createKeyword(2L, "keyword2", "title2", false)
        );
        when(toastBoxRepository.findAll()).thenReturn(keywords);

        // when
        List<ToastBoxKeywordDTO> result = toastBoxService.getAllToastBoxWords();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKeyword()).isEqualTo("keyword1");
        assertThat(result.get(1).getKeyword()).isEqualTo("keyword2");
    }

    @Test
    @DisplayName("키워드 추가 성공")
    void addToastBoxKeyword_Success() {
        // given
        ToastBoxKeywordRequest request = ToastBoxKeywordRequest.builder()
                .keyword("newKeyword")
                .title("New Title")
                .content("New Content")
                .linkUrl("http://example.com")
                .imageUrl("http://example.com/image.jpg")
                .build();
        when(toastBoxRepository.findByKeyword("newKeyword")).thenReturn(Optional.empty());
        ToastBoxKeyword savedKeyword = mock(ToastBoxKeyword.class);

        when(savedKeyword.getId()).thenReturn(1L);
        when(savedKeyword.getKeyword()).thenReturn("newKeyword");
        when(toastBoxRepository.save(any(ToastBoxKeyword.class))).thenReturn(savedKeyword);

        // when
        ToastBoxKeywordDTO result = toastBoxService.addToastBoxKeyword(request);

        // then
        assertThat(result.getKeyword()).isEqualTo("newKeyword");
        verify(toastBoxRepository).findByKeyword("newKeyword");
        verify(toastBoxRepository).save(any(ToastBoxKeyword.class));
    }

    @Test
    @DisplayName("중복 키워드 추가 시 예외 발생")
    void addToastBoxKeyword_DuplicateKeyword() {
        // given
        ToastBoxKeywordRequest request = ToastBoxKeywordRequest.builder()
                .keyword("existingKeyword")
                .build();

        when(toastBoxRepository.findByKeyword("existingKeyword")).thenReturn(Optional.of(mock(ToastBoxKeyword.class)));

        // when & then
        assertThrows(CustomException.class, () -> toastBoxService.addToastBoxKeyword(request));
        verify(toastBoxRepository, never()).save(any(ToastBoxKeyword.class));
    }

    @Test
    @DisplayName("키워드 업데이트 성공")
    void updateToastBoxKeyword_Success() {
        // given
        Long id = 1L;
        ToastBoxKeywordDTO dto = ToastBoxKeywordDTO.builder()
                .id(id)
                .keyword("updatedKeyword")
                .title("Updated Title")
                .content("Updated Content")
                .linkUrl("http://updated.com")
                .imageUrl("http://updated.com/image.jpg")
                .build();

        ToastBoxKeyword existingKeyword = mock(ToastBoxKeyword.class);
        when(existingKeyword.getKeyword()).thenReturn("oldKeyword");
        when(toastBoxRepository.findById(id)).thenReturn(Optional.of(existingKeyword));
        when(toastBoxRepository.findByKeyword("updatedKeyword")).thenReturn(Optional.empty());
        when(toastBoxRepository.save(existingKeyword)).thenReturn(existingKeyword);

        // when
        toastBoxService.updateToastBoxKeyword(id, dto);

        // then
        verify(existingKeyword).update(
                dto.getKeyword(),
                dto.getTitle(),
                dto.getContent(),
                dto.getLinkUrl(),
                dto.getImageUrl()
        );
        verify(toastBoxRepository).save(existingKeyword);
    }

    @Test
    @DisplayName("키워드 업데이트 시 중복 키워드 예외 발생")
    void updateToastBoxKeyword_DuplicateKeyword() {
        // given
        Long id = 1L;
        ToastBoxKeywordDTO dto = ToastBoxKeywordDTO.builder()
                .id(id)
                .keyword("duplicateKeyword")
                .build();

        ToastBoxKeyword existingKeyword = mock(ToastBoxKeyword.class);
        when(existingKeyword.getKeyword()).thenReturn("oldKeyword");
        when(toastBoxRepository.findById(id)).thenReturn(Optional.of(existingKeyword));
        when(toastBoxRepository.findByKeyword("duplicateKeyword")).thenReturn(Optional.of(mock(ToastBoxKeyword.class)));

        // when & then
        assertThrows(CustomException.class, () -> toastBoxService.updateToastBoxKeyword(id, dto));
        verify(existingKeyword, never()).update(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("키워드 삭제 성공")
    void deleteToastBoxKeyword_Success() {
        // given
        Long id = 1L;
        when(toastBoxRepository.existsById(id)).thenReturn(true);

        // when
        toastBoxService.deleteToastBoxKeyWord(id);

        // then
        verify(toastBoxRepository).deleteById(id);
    }

    @Test
    @DisplayName("존재하지 않은 키워드 삭제 시 예외")
    void deleteToastBoxKeyword_NotFound() {
        // given
        Long id = 1L;
        when(toastBoxRepository.existsById(id)).thenReturn(false);

        // when & then
        assertThrows(CustomException.class, () -> toastBoxService.deleteToastBoxKeyWord(id));
        verify(toastBoxRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("키워드 활성화 상태 변경 성공")
    void setToastBoxKeywordActive_Success() {
        // given
        Long id = 1L;
        boolean newActiveStatus = false;

        ToastBoxKeyword keyword = mock(ToastBoxKeyword.class);
        when(toastBoxRepository.findById(id)).thenReturn(Optional.of(keyword));
        when(toastBoxRepository.save(keyword)).thenReturn(keyword);

        // when
        toastBoxService.setToastBoxKeywordActive(id, newActiveStatus);

        // then
        verify(keyword).setActive(newActiveStatus);
        verify(toastBoxRepository).save(keyword);
    }

    @Test
    @DisplayName("ID로 키워드 조회 성공")
    void findToastBoxKeywordById_Success() {
        // given
        Long id = 1L;
        ToastBoxKeyword keyword = mock(ToastBoxKeyword.class);
        when(toastBoxRepository.findById(id)).thenReturn(Optional.of(keyword));

        // when
        ToastBoxKeyword result = toastBoxService.findToastBoxKeywordById(id);

        // then
        assertThat(result).isEqualTo(keyword);
    }

    @Test
    @DisplayName("존재하지 않는 Id로 키워드 조회 시 예외")
    void findToastBoxKeywordById_NotFound() {
        // given
        Long id = 999L;
        when(toastBoxRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CustomException.class, () -> toastBoxService.findToastBoxKeywordById(id));
    }
}