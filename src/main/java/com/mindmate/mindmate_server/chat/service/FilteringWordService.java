package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.dto.FilteringWordDTO;
import com.mindmate.mindmate_server.chat.repository.FilteringWordRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.FilteringErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilteringWordService {
    private final FilteringWordRepository filteringWordRepository;
    private final ContentFilterService contentFilterService;

    public List<FilteringWordDTO> getAllFilteringWords() {
        return filteringWordRepository.findAll().stream()
                .map(FilteringWordDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FilteringWordDTO addFilteringWord(String word) {
        if (filteringWordRepository.findByWord(word).isPresent()) {
            throw new CustomException(FilteringErrorCode.DUPLICATE_FILTERING_WORD);
        }

        FilteringWord filteringWord = FilteringWord.builder()
                .word(word)
                .build();

        FilteringWord save = filteringWordRepository.save(filteringWord);

        // todo: 이거를 바로바로 업데이트 시킬 지 아니면 스케줄로 처리할 지 고민 -> 만약 바로 할거면 아래에 다 추가해야함
        contentFilterService.refreshFilteringWords();
        log.info("필터링 단어 추가: {}", word);
        return FilteringWordDTO.from(save);
    }

    @Transactional
    public void deleteFilteringWord(Long id) {
        if (!filteringWordRepository.existsById(id)) {
            throw new CustomException(FilteringErrorCode.FILTERING_WORD_NOT_FOUND);
        }
        filteringWordRepository.deleteById(id);

        // 추가
    }

    @Transactional
    public FilteringWordDTO activateFilteringWod(Long id) {
        FilteringWord word = findFilteringWordById(id);

        word.activate();
        FilteringWord save = filteringWordRepository.save(word);
        // 추가

        log.info("필터링 단어 활성화: ID={}, 단어={}", id, word.getWord());
        return FilteringWordDTO.from(save);
    }

    @Transactional
    public FilteringWordDTO deactivateFilteringWod(Long id) {
        FilteringWord word = findFilteringWordById(id);

        word.deactivate();
        FilteringWord save = filteringWordRepository.save(word);

        // 추가
        log.info("필터링 단어 비활성화: ID={}, 단어={}", id, word.getWord());
        return FilteringWordDTO.from(save);
    }

    public FilteringWord findFilteringWordById(Long id) {
        return filteringWordRepository.findById(id)
                .orElseThrow(() -> new CustomException(FilteringErrorCode.FILTERING_WORD_NOT_FOUND));
    }
}
