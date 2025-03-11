package com.mindmate.mindmate_server.chat.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public enum FilteringWordCategory {
    PROFANITY("욕설", Arrays.asList("욕설1", "욕설2", "욕설3")),
    DISCRIMINATION("차별", Arrays.asList("차별단어1", "차별단어2")),
    ADULT("성인", Arrays.asList("성인단어1", "성인단어2")),
    PERSONAL_INFO("개인정보", Arrays.asList("주민번호", "전화번호"));

    private final String description;
    private final List<String> words;

    FilteringWordCategory(String description, List<String> words) {
        this.description = description;
        this.words = words;
    }

    public static List<String> getAllBannedWords() {
        return Arrays.stream(values())
                .flatMap(category -> category.getWords().stream())
                .collect(Collectors.toList());
    }

    public boolean containsWord(String text) {
        return words.stream().anyMatch(text::contains);
    }

    public static Optional<FilteringWordCategory> findMatchingCategory(String text) {
        return Arrays.stream(values())
                .filter(category -> category.containsWord(text))
                .findFirst();
    }
}
