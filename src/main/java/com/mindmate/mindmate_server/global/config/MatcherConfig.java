package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.chat.service.AhoCorasickMatcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatcherConfig {
    @Bean
    public AhoCorasickMatcher filteringMatcher() {
        return new AhoCorasickMatcher();
    }

    @Bean
    @Qualifier("toastBoxMatcher")
    public AhoCorasickMatcher toastBoxMatcher() {
        return new AhoCorasickMatcher();
    }
}
