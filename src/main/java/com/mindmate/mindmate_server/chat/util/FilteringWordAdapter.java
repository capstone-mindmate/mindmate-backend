package com.mindmate.mindmate_server.chat.util;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.service.AhoCorasickMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilteringWordAdapter {
    private final AhoCorasickMatcher<FilteringWord> matcher;

    public FilteringWordAdapter(AhoCorasickMatcher<FilteringWord> matcher) {
        this.matcher = matcher;
    }

    public void initialize(List<FilteringWord> keywords) {
        matcher.initialize(keywords, FilteringWord::getWord, FilteringWord::isActive);
    }

    public boolean isFiltered(String content) {
        return matcher.findFirstMatch(content).isPresent();
    }

}
