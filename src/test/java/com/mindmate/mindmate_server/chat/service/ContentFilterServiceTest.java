package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.repository.FilteringWordRepository;
import com.mindmate.mindmate_server.chat.util.FilteringWordAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentFilterServiceTest {
    @Mock private FilteringWordRepository filteringWordRepository;
    @Mock private FilteringWordAdapter filteringWordAdapter;

    @InjectMocks
    private ContentFilterService contentFilterService;

    @ParameterizedTest(name = "isFiltered: \"{0}\" → {1}")
    @CsvSource({
            "null, false",
            "'', false",
            "'  ', false"
    })
    @DisplayName("null 또는 빈 문자열은 필터링되지 않음")
    void isFiltered_NullOrEmpty_ReturnsFalse(String input, boolean expected) {
        assertEquals(expected, contentFilterService.isFiltered(input == null ? null : input));
    }

    @ParameterizedTest(name = "isFiltered: \"{0}\" → {1}")
    @CsvSource({
            "'욕설이 포함된 문장', true",
            "'정상적인 문장입니다.', false"
    })
    @DisplayName("필터링 단어 포함 여부에 따라 true/false 반환")
    void isFiltered_FilteredOrNot(String input, boolean expected) {
        when(filteringWordAdapter.isFiltered(input)).thenReturn(expected);
        assertEquals(expected, contentFilterService.isFiltered(input));
    }

    @Test
    @DisplayName("필터링 단어 갱신")
    void refreshFilteringWords_InitializeAdapter() {
        // given
        List<FilteringWord> words = List.of(
                new FilteringWord("욕설1"),
                new FilteringWord("욕설2")
        );
        when(filteringWordRepository.findByActiveTrue()).thenReturn(words);

        // when
        contentFilterService.refreshFilteringWords();

        // then
        verify(filteringWordAdapter).initialize(words);
    }

}
