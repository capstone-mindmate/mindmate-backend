package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.service.AhoCorasickMatcher;
import com.mindmate.mindmate_server.chat.util.FilteringWordAdapter;
import com.mindmate.mindmate_server.chat.util.ToastBoxAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatcherConfig {
    @Bean
    public AhoCorasickMatcher<FilteringWord> filteringMatcher() {
        return new AhoCorasickMatcher();
    }

    @Bean
    @Qualifier("toastBoxMatcher")
    public AhoCorasickMatcher<ToastBoxKeyword> toastBoxMatcher() {
        return new AhoCorasickMatcher();
    }

    @Bean
    public FilteringWordAdapter filteringWordAdapter(AhoCorasickMatcher<FilteringWord> filteringMatcher) {
        return new FilteringWordAdapter(filteringMatcher);
    }

    @Bean
    public ToastBoxAdapter toastBoxAdapter(@Qualifier("toastBoxMatcher") AhoCorasickMatcher<ToastBoxKeyword> toastBoxMatcher) {
        return new ToastBoxAdapter(toastBoxMatcher);
    }
}
