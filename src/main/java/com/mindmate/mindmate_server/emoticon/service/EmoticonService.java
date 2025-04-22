package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonDetailResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.dto.UserEmoticonResponse;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.emoticon.repository.UserEmoticonRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.EmoticonErrorCode;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmoticonService {
    private final EmoticonRepository emoticonRepository;
    private final UserEmoticonRepository userEmoticonRepository;
    private final UserService userService;


    public List<EmoticonResponse> getShopEmoticons(Long userId) {
        List<Emoticon> emoticons = emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT);

        Set<Long> purchasedEmotionIds = new HashSet<>();
        if (userId != null) {
            purchasedEmotionIds = userEmoticonRepository.findByUserId(userId)
                    .stream()
                    .map(ue -> ue.getEmoticon().getId())
                    .collect(Collectors.toSet());
        }

        List<EmoticonResponse> result = new ArrayList<>();
        for (Emoticon emoticon : emoticons) {
            boolean isPurchased = purchasedEmotionIds.contains(emoticon.getId());
            result.add(EmoticonResponse.from(emoticon, isPurchased));
        }

        return result;
    }

    public EmoticonDetailResponse getEmoticonDetail(Long emoticonId, Long userId) {
        Emoticon emoticon = findEmoticonById(emoticonId);

        boolean isPurchased = false;
        if (userId != null) {
            isPurchased = userEmoticonRepository.existsByUserIdAndEmoticonId(userId, emoticonId);
        }

        // todo: 어떤거 유사한 이모티콘들을 보여줄지? 일단 지금은 가격 기준
        int priceRange = 100;
        List<Emoticon> similarPriceEmoticons = emoticonRepository.findByStatusAndIsDefaultOrderByCreatedAtDesc(
                        EmoticonStatus.ACCEPT, emoticon.isDefault())
                .stream()
                .filter(e -> e.getId() != emoticonId)
                .filter(e -> Math.abs(e.getPrice() - emoticon.getPrice()) <= priceRange)
                .limit(10)
                .collect(Collectors.toList());

        List<EmoticonResponse> similarEmoticonResponses = new ArrayList<>();
        if (userId != null) {
            Set<Long> purchasedEmoticonIds = userEmoticonRepository.findByUserId(userId)
                    .stream()
                    .map(ue -> ue.getEmoticon().getId())
                    .collect(Collectors.toSet());

            for (Emoticon similar : similarPriceEmoticons) {
                boolean isSimilarPurchased = purchasedEmoticonIds.contains(similar.getId());
                similarEmoticonResponses.add(EmoticonResponse.from(similar, isSimilarPurchased));
            }
        } else {
            similarEmoticonResponses = similarPriceEmoticons.stream()
                    .map(EmoticonResponse::from)
                    .collect(Collectors.toList());
        }

        return EmoticonDetailResponse.builder()
                .emoticon(EmoticonResponse.from(emoticon, isPurchased))
                .similarEmoticons(similarEmoticonResponses)
                .build();
    }

    public UserEmoticonResponse getUserEmoticons(Long userId) {
        List<UserEmoticon> userEmoticons = userEmoticonRepository.findByUserId(userId);

        Set<Long> ownedEmoticonIds = userEmoticons.stream()
                .map(ue -> ue.getEmoticon().getId())
                .collect(Collectors.toSet());

        List<Emoticon> allEmoticons = emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT);

        List<EmoticonResponse> ownedEmoticons = new ArrayList<>();
        List<EmoticonResponse> notOwnedEmoticons = new ArrayList<>();

        for (Emoticon emoticon : allEmoticons) {
            if (ownedEmoticonIds.contains(emoticon.getId())) {
                ownedEmoticons.add(EmoticonResponse.from(emoticon));
            } else {
                notOwnedEmoticons.add(EmoticonResponse.from(emoticon));
            }
        }

        return UserEmoticonResponse.builder()
                .ownedEmoticons(ownedEmoticons)
                .notOwnedEmoticons(notOwnedEmoticons)
                .build();
    }

    public List<EmoticonResponse> getAvailableEmoticons(Long userId) {
        List<UserEmoticon> userEmoticons = userEmoticonRepository.findByUserId(userId);

        return userEmoticons.stream()
                .map(ue -> EmoticonResponse.from(ue.getEmoticon()))
                .collect(Collectors.toList());
    }

    public Emoticon findEmoticonById(Long emoticonId) {
        return emoticonRepository.findById(emoticonId)
                .orElseThrow(() -> new CustomException(EmoticonErrorCode.EMOTICON_NOT_FOUND));
    }
}
