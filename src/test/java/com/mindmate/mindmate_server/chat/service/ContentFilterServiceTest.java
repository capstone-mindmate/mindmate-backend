package com.mindmate.mindmate_server.chat.service;


import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ContentFilterServiceTest {
    private ContentFilterService contentFilterService;

    @BeforeEach
    void setup() {
        contentFilterService = new ContentFilterService();
    }

    @Nested
    @DisplayName("금지어 포함 여부 확인")
    class ContainFilteringWordTest {
        @Test
        @DisplayName("금지어 포함 여부 확인 - 포함")
        void containsFilteringWord_True() {
            // given
            String content = "욕설1 금지어가 포함되어 있습니다";

            // when
            boolean result = contentFilterService.containsFilteringWord(content);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("금지어 포함 여부 확인 - 미포함")
        void containsFilteringWord_False() {
            // given
            String content = " 금지어가 포함되어 있지 않습니다";

            // when
            boolean result = contentFilterService.containsFilteringWord(content);

            // then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("금지어 카테고리 찾기")
    class FindFilteringWordCategoryTest {
        @Test
        @DisplayName("금지어 카테고리 찾기 - 욕설 카테고리")
        void findFilteringWordCategory_Profanity() {
            // given
            String content = "이 내용에는 욕설1이 포함되어 있습니다.";

            // when
            Optional<FilteringWordCategory> result = contentFilterService.findFilteringWordCategory(content);

            // then
            assertTrue(result.isPresent());
            assertEquals(FilteringWordCategory.PROFANITY, result.get());
            assertEquals("욕설", result.get().getDescription());
        }

        @Test
        @DisplayName("금지어 카테고리 찾기 - 차별 카테고리")
        void findFilteringWordCategory_Discrimination() {
            // given
            String content = "이 내용에는 차별단어1이 포함되어 있습니다.";

            // when
            Optional<FilteringWordCategory> result = contentFilterService.findFilteringWordCategory(content);

            // then
            assertTrue(result.isPresent());
            assertEquals(FilteringWordCategory.DISCRIMINATION, result.get());
            assertEquals("차별", result.get().getDescription());
        }

        @Test
        @DisplayName("금지어 카테고리 찾기 - 카테고리 없음")
        void findFilteringWordCategory_NotFound() {
            // given
            String content = "이 내용에는 금지어가 포함되어 있지 않습니다.";

            // when
            Optional<FilteringWordCategory> result = contentFilterService.findFilteringWordCategory(content);

            // then
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("금지어 마스킹")
    class MaskFilteringWordTest {
        @Test
        @DisplayName("금지어 마스킹 - 단일 금지어")
        void maskFilteringWords_SingleWord() {
            // given
            String content = "이 내용에는 욕설1이 포함되어 있습니다.";

            // when
            String result = contentFilterService.maskFilteringWords(content);

            // then
            assertNotNull(result);
            assertFalse(result.contains("욕설1"));
            assertTrue(result.contains("***")); // 욕설1이 ***로 마스킹됨
            assertEquals("이 내용에는 ***이 포함되어 있습니다.", result);
        }

        @Test
        @DisplayName("금지어 마스킹 - 여러 금지어")
        void maskFilteringWords_MultipleWords() {
            // given
            String content = "이 내용에는 욕설1과 차별단어1이 포함되어 있습니다.";

            // when
            String result = contentFilterService.maskFilteringWords(content);

            // then
            assertNotNull(result);
            assertFalse(result.contains("욕설1"));
            assertFalse(result.contains("차별단어1"));
            assertTrue(result.contains("***")); // 욕설1이 ***로 마스킹됨
            assertTrue(result.contains("*****")); // 차별단어1이 *****로 마스킹됨
            assertEquals("이 내용에는 ***과 *****이 포함되어 있습니다.", result);
        }

        @Test
        @DisplayName("금지어 마스킹 - 금지어 없음")
        void maskFilteringWords_NoFilteringWords() {
            // given
            String content = "이 내용에는 금지어가 포함되어 있지 않습니다.";

            // when
            String result = contentFilterService.maskFilteringWords(content);

            // then
            assertNotNull(result);
            assertEquals(content, result); // 원본 내용과 동일
        }
    }



}
