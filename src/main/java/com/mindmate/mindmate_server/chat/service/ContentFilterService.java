package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.repository.FilteringWordRepository;
import com.mindmate.mindmate_server.chat.util.FilteringWordAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentFilterService {
    private final FilteringWordRepository filteringWordRepository;
    private final FilteringWordAdapter filteringWordAdapter;

    @PostConstruct
    public void initialize() {
        refreshFilteringWords();
    }

    @Scheduled(fixedRate = 86400000) // 24시간마다 갱신
    public void refreshFilteringWords() {
        List<FilteringWord> activeWords = filteringWordRepository.findByActiveTrue();
        filteringWordAdapter.initialize(activeWords);
        log.info("필터링 단어 목록 갱신 완료: {} 개", activeWords.size());
    }



    public boolean isFiltered(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        return filteringWordAdapter.isFiltered(content);
    }
}
