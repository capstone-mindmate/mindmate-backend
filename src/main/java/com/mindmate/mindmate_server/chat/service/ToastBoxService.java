package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.repository.ToastBoxRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToastBoxService {
    private final ToastBoxRepository toastBoxRepository;

    @Qualifier("toastBoxMatcher")
    private final AhoCorasickMatcher toastBoxMatcher;

    @PostConstruct
    public void initialize() {
        refreshToastBoxKeywords();
    }

    @Scheduled(fixedRate = 86400000) // 24시간마다 갱신
    public void refreshToastBoxKeywords() {
        List<ToastBoxKeyword> activeKeywords = toastBoxRepository.findByActiveTrue();

        List<FilteringWord> keywords = activeKeywords.stream()
                .map(keyword -> FilteringWord.builder()
                        .word(keyword.getKeyword())
                        .build())
                .collect(Collectors.toList());

        toastBoxMatcher.initialize(keywords);
    }

}
