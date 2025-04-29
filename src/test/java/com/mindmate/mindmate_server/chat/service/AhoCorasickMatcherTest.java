package com.mindmate.mindmate_server.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AhoCorasickMatcherTest {
    static class SimpleFilteringWord {
        private final String word;
        private final boolean active;

        public SimpleFilteringWord(String word, boolean active) {
            this.word = word;
            this.active = active;
        }

        public String getWord() { return word; }
        public boolean isActive() { return active; }
    }

    private AhoCorasickMatcher<SimpleFilteringWord> matcher;

    @BeforeEach
    void setup() {
        matcher = new AhoCorasickMatcher<>();
    }

    @Test
    @DisplayName("초기화하지 않으면 예외 발생")
    void search_WithoutInitialize_ThrowsException() {
        assertThrows(IllegalStateException.class, () -> matcher.search("test"));
    }

    @Test
    @DisplayName("한 단어 필터링")
    void search_SingleWord() {
        // given
        List<SimpleFilteringWord> words = List.of(
                new SimpleFilteringWord("욕설", true)
        );
        matcher.initialize(words, SimpleFilteringWord::getWord, SimpleFilteringWord::isActive);

        // when
        List<String> result = matcher.search("이 문장 욕설 포함");

        // then
        assertTrue(result.contains("욕설"));
    }

    @Test
    @DisplayName("여러 단어 필터링 및 비활성 단어 무시")
    void search_MultipleWordsAndInactive() {
        // given
        List<SimpleFilteringWord> words = List.of(
                new SimpleFilteringWord("나쁜말", true),
                new SimpleFilteringWord("비속어", false),
                new SimpleFilteringWord("욕", true)
        );
        matcher.initialize(words, SimpleFilteringWord::getWord, SimpleFilteringWord::isActive);
        
        // when
        List<String> result = matcher.search("나쁜말 욕 필터링. 비속어 비활성 상태");
        
        // then
        assertTrue(result.contains("나쁜말"));
        assertTrue(result.contains("욕"));
        assertFalse(result.contains("비속어"));
    }

    @Test
    @DisplayName("첫 매칭 단어 객체 반환")
    void findFirstMatchItem_ReturnsFirstMatch() {
        // given
        List<SimpleFilteringWord> words = List.of(
                new SimpleFilteringWord("욕", true),
                new SimpleFilteringWord("나쁜말", true)
        );
        matcher.initialize(words, SimpleFilteringWord::getWord, SimpleFilteringWord::isActive);

        // when
        Optional<SimpleFilteringWord> match = matcher.findFirstMatchItem("여기 나쁜말 존재");

        // then
        assertTrue(match.isPresent());
        assertEquals("나쁜말", match.get().getWord());
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트/Optional 반환")
    void search_NoMatch_ReturnsEmpty() {
        // given
        List<SimpleFilteringWord> words = List.of(
                new SimpleFilteringWord("욕", true)
        );
        matcher.initialize(words, SimpleFilteringWord::getWord, SimpleFilteringWord::isActive);

        // when & then
        assertTrue(matcher.search("정상 문장").isEmpty());
        assertTrue(matcher.findFirstMatch("정상 문장").isEmpty());
        assertTrue(matcher.findFirstMatchItem("정상 문장").isEmpty());
    }


}