package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordDTO;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordRequest;
import com.mindmate.mindmate_server.chat.repository.ToastBoxRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.FilteringErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ToastBoxServiceImpl implements ToastBoxService {
    private final ToastBoxRepository toastBoxRepository;

    @Qualifier("toastBoxMatcher")
    private final AhoCorasickMatcher toastBoxMatcher;

    @PostConstruct
    public void initialize() {
        refreshToastBoxKeywords();
    }

    @Scheduled(fixedRate = 86400000) // 24시간마다 갱신
    @Override
    public void refreshToastBoxKeywords() {
        List<ToastBoxKeyword> activeKeywords = toastBoxRepository.findByActiveTrue();

        List<FilteringWord> keywords = activeKeywords.stream()
                .map(keyword -> FilteringWord.builder()
                        .word(keyword.getKeyword())
                        .build())
                .collect(Collectors.toList());

        toastBoxMatcher.initialize(keywords);
    }

    @Override
    public List<ToastBoxKeyword> findToastBoxKeywords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> matchedKeywords = toastBoxMatcher.search(content);

        if (matchedKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> uniqueKeywords = new HashSet<>(matchedKeywords);
        return toastBoxRepository.findByKeywordInAndActiveTrue(uniqueKeywords);
    }

    @Override
    public List<ToastBoxKeywordDTO> getAllToastBoxWords() {
        return toastBoxRepository.findAll().stream()
                .map(ToastBoxKeywordDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ToastBoxKeywordDTO addToastBoxKeyword(ToastBoxKeywordRequest dto) {
        if (toastBoxRepository.findByKeyword(dto.getKeyword()).isPresent()) {
            throw new CustomException(FilteringErrorCode.DUPLICATE_TOAST_BOX_KEYWORD);
        }

        ToastBoxKeyword keyword = ToastBoxKeyword.builder()
                .keyword(dto.getKeyword())
                .title(dto.getTitle())
                .content(dto.getContent())
                .linkUrl(dto.getLinkUrl())
                .imageUrl(dto.getImageUrl())
                .active(true)
                .build();

        ToastBoxKeyword save = toastBoxRepository.save(keyword);
        // todo: 필터링 키워드 때랑 마찬가지
        refreshToastBoxKeywords();
        return ToastBoxKeywordDTO.from(save);
    }

    @Override
    @Transactional
    public ToastBoxKeywordDTO updateToastBoxKeyword(Long id, ToastBoxKeywordDTO dto) {
        ToastBoxKeyword keyword = findToastBoxKeywordById(id);

        if (!keyword.getKeyword().equals(dto.getKeyword())) {
            if (toastBoxRepository.findByKeyword(dto.getKeyword()).isPresent()) {
                throw new CustomException(FilteringErrorCode.DUPLICATE_TOAST_BOX_KEYWORD);
            }
        }

        keyword.update(dto.getKeyword(), dto.getTitle(), dto.getContent(), dto.getLinkUrl(), dto.getImageUrl());

        ToastBoxKeyword save = toastBoxRepository.save(keyword);
        // 추가
        return ToastBoxKeywordDTO.from(save);
    }

    @Override
    @Transactional
    public void deleteToastBoxKeyWord(Long id) {
        if (!toastBoxRepository.existsById(id)) {
            throw new CustomException(FilteringErrorCode.TOAST_BOX_KEYWORD_NOT_FOUND);
        }

        toastBoxRepository.deleteById(id);
        // 추가
    }

    @Override
    @Transactional
    public ToastBoxKeywordDTO setToastBoxKeywordActive(Long id, boolean active) {
        ToastBoxKeyword keyword = findToastBoxKeywordById(id);

        keyword.setActive(active);
        ToastBoxKeyword save = toastBoxRepository.save(keyword);
        // 추가
        return ToastBoxKeywordDTO.from(save);
    }

    @Override
    public ToastBoxKeyword findToastBoxKeywordById(Long id) {
        return toastBoxRepository.findById(id)
                .orElseThrow(() -> new CustomException(FilteringErrorCode.TOAST_BOX_KEYWORD_NOT_FOUND));
    }

}
