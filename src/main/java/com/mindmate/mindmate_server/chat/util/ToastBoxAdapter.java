package com.mindmate.mindmate_server.chat.util;

import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.service.AhoCorasickMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ToastBoxAdapter {
    private final AhoCorasickMatcher<ToastBoxKeyword> matcher;

    public ToastBoxAdapter(AhoCorasickMatcher<ToastBoxKeyword> matcher) {
        this.matcher = matcher;
    }

    public void initialize(List<ToastBoxKeyword> keywords) {
        matcher.initialize(keywords, ToastBoxKeyword::getKeyword, ToastBoxKeyword::isActive);
    }

    public List<ToastBoxKeyword> findMatchingKeywords(String content) {
        return matcher.searchItems(content);
    }
}
