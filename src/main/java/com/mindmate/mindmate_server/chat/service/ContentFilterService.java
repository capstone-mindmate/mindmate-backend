package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ContentFilterService {
    public boolean containsFilteringWord(String content) {
        return FilteringWordCategory.getAllBannedWords().stream()
                .anyMatch(content::contains);
    }

    public Optional<FilteringWordCategory> findFilteringWordCategory(String content) {
        return FilteringWordCategory.findMatchingCategory(content);
    }

    public String maskFilteringWords(String content) {
        String maskContent = content;
        for (String word : FilteringWordCategory.getAllBannedWords()) {
            if (content.contains(word)) {
                maskContent = maskContent.replace(word, "*".repeat(word.length()));
            }
        }
        return maskContent;
    }
}
