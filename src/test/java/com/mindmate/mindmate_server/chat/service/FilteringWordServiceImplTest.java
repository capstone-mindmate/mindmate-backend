package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.dto.FilteringWordDTO;
import com.mindmate.mindmate_server.chat.repository.FilteringWordRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilteringWordServiceImplTest {
    @Mock private FilteringWordRepository filteringWordRepository;

    @InjectMocks
    private FilteringWordServiceImpl filteringWordService;

    @Test
    @DisplayName("전체 필터링 단어 조회")
    void getAllFilteringWords_Success() {
        // given
        FilteringWord word1 = FilteringWord.builder().word("금지1").build();
        FilteringWord word2 = FilteringWord.builder().word("금지2").build();
        when(filteringWordRepository.findAll()).thenReturn(List.of(word1, word2));

        // when
        List<FilteringWordDTO> result = filteringWordService.getAllFilteringWords();

        // then
        assertEquals(2, result.size());
        assertEquals("금지1", result.get(0).getWord());
        assertEquals("금지2", result.get(1).getWord());
    }

    @ParameterizedTest(name = "addFilteringWord - {0} (중복: {1})")
    @CsvSource({
            "새금지, false",
            "중복, true"
    })
    void addFilteringWord_Param(String word, boolean isDuplicated) {
        // given
        if (isDuplicated) {
            when(filteringWordRepository.findByWord(word))
                    .thenReturn(Optional.of(FilteringWord.builder().word(word).build()));
        } else {
            when(filteringWordRepository.findByWord(word)).thenReturn(Optional.empty());
            FilteringWord saved = FilteringWord.builder().word(word).build();
            when(filteringWordRepository.save(any(FilteringWord.class))).thenReturn(saved);
        }

        // when & then
        if (isDuplicated) {
            assertThrows(CustomException.class, () -> filteringWordService.addFilteringWord(word));
            verify(filteringWordRepository, never()).save(any());
        } else {
            FilteringWordDTO result = filteringWordService.addFilteringWord(word);
            assertEquals(word, result.getWord());
            verify(filteringWordRepository).save(any(FilteringWord.class));
        }
    }

    @ParameterizedTest(name = "deleteFilteringWord - id={0}, exists={1}")
    @CsvSource({
            "10, true",
            "20, false"
    })
    void deleteFilteringWord_Param(Long id, boolean exists) {
        // given
        when(filteringWordRepository.existsById(id)).thenReturn(exists);

        // when & then
        if (exists) {
            filteringWordService.deleteFilteringWord(id);
            verify(filteringWordRepository).deleteById(id);
        } else {
            assertThrows(CustomException.class, () -> filteringWordService.deleteFilteringWord(id));
            verify(filteringWordRepository, never()).deleteById(id);
        }
    }

    @ParameterizedTest(name = "setFilteringWordActive - id={0}, exists={1}, active={2}")
    @MethodSource("setFilteringWordActiveCases")
    void setFilteringWordActive_Param(Long id, boolean exists, boolean active) {
        // given
        FilteringWord word = buildFilteringWordWithId("활성화", id);
        if (exists) {
            when(filteringWordRepository.findById(id)).thenReturn(Optional.of(word));
            when(filteringWordRepository.save(word)).thenReturn(word);
        } else {
            when(filteringWordRepository.findById(id)).thenReturn(Optional.empty());
        }

        // when & then
        if (exists) {
            FilteringWordDTO result = filteringWordService.setFilteringWordActive(id, active);
            assertEquals(active, word.isActive());
            assertEquals("활성화", result.getWord());
            verify(filteringWordRepository).save(word);
        } else {
            assertThrows(CustomException.class, () -> filteringWordService.setFilteringWordActive(id, active));
        }
    }

    static Stream<Arguments> setFilteringWordActiveCases() {
        return Stream.of(
                Arguments.of(30L, true, false),
                Arguments.of(40L, false, true)
        );
    }

    @ParameterizedTest(name = "findFilteringWordById - id={0}, exists={1}")
    @CsvSource({
            "50, true",
            "60, false"
    })
    void findFilteringWordById_Param(Long id, boolean exists) {
        // given
        FilteringWord word = FilteringWord.builder().word("조회").build();
        if (exists) {
            when(filteringWordRepository.findById(id)).thenReturn(Optional.of(word));
        } else {
            when(filteringWordRepository.findById(id)).thenReturn(Optional.empty());
        }

        // when & then
        if (exists) {
            FilteringWord result = filteringWordService.findFilteringWordById(id);
            assertEquals("조회", result.getWord());
        } else {
            assertThrows(CustomException.class, () -> filteringWordService.findFilteringWordById(id));
        }
    }

    private FilteringWord buildFilteringWordWithId(String word, Long id) {
        FilteringWord filteringWord = FilteringWord.builder().word(word).build();
        try {
            Field idField = FilteringWord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(filteringWord, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return filteringWord;
    }
}