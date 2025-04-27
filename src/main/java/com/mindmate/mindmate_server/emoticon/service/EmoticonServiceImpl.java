package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonType;
import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonDetailResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.dto.UserEmoticonResponse;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.emoticon.repository.UserEmoticonRepository;
import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.EmoticonErrorCode;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.dto.PointUseRequest;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmoticonServiceImpl implements EmoticonService {
    private final EmoticonRepository emoticonRepository;
    private final UserEmoticonRepository userEmoticonRepository;

    private final UserService userService;
    private final PointService pointService;
    private final SlackNotifier slackNotifier;
    private final FileStorageService fileStorageService;


    @Value("${emoticon.dir}")
    private String emoticonDir;

    @Value("${emoticon.url.prefix}")
    private String emoticonUrlPrefix;

    @Override
    public List<EmoticonResponse> getShopEmoticons(Long userId) {
        List<Emoticon> emoticons = emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT);

        Set<Long> purchasedEmotionIds = userId != null
                ? getOwnedEmoticonIds(userId)
                : Collections.emptySet();

        return emoticons.stream()
                .map(emoticon -> EmoticonResponse.from(
                        emoticon,
                        purchasedEmotionIds.contains(emoticon.getId())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public EmoticonDetailResponse getEmoticonDetail(Long emoticonId, Long userId) {
        Emoticon emoticon = findEmoticonById(emoticonId);

        boolean isPurchased = userId != null && isEmoticonOwnedByUser(userId, emoticonId);

        // todo: 어떤거 유사한 이모티콘들을 보여줄지? 일단 지금은 가격 기준
        int priceRange = 100;
        List<Emoticon> similarPriceEmoticons = emoticonRepository.findSimilarPriceEmoticons(
                EmoticonStatus.ACCEPT,
                emoticon.isDefault(),
                emoticonId,
                emoticon.getPrice(),
                priceRange,
                PageRequest.of(0, 10)
        );


        List<EmoticonResponse> similarEmoticonResponses;
        if (userId != null) {
            Set<Long> ownedIds = getOwnedEmoticonIds(userId);
            similarEmoticonResponses = similarPriceEmoticons.stream()
                    .map(e -> EmoticonResponse.from(e, ownedIds.contains(e.getId())))
                    .collect(Collectors.toList());
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

    @Override
    public UserEmoticonResponse getUserEmoticons(Long userId) {
        Set<Long> ownedEmoticonIds = getOwnedEmoticonIds(userId);
        List<Emoticon> allEmoticons = emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT);

        Map<Boolean, List<EmoticonResponse>> partitionedEmoticons = allEmoticons.stream()
                .map(EmoticonResponse::from)
                .collect(Collectors.partitioningBy(
                        emoticon -> ownedEmoticonIds.contains(emoticon.getId())
                ));

        return UserEmoticonResponse.builder()
                .ownedEmoticons(partitionedEmoticons.get(true))
                .notOwnedEmoticons(partitionedEmoticons.get(false))
                .build();
    }

    @Override
    public Set<Long> getOwnedEmoticonIds(Long userId) {
        if (userId == null) return Collections.emptySet();

        return userEmoticonRepository.findByUserId(userId).stream()
                .map(ue -> ue.getEmoticon().getId())
                .collect(Collectors.toSet());
    }

    @Override
    public List<EmoticonResponse> getAvailableEmoticons(Long userId) {
        List<UserEmoticon> userEmoticons = userEmoticonRepository.findByUserId(userId);

        return userEmoticons.stream()
                .map(ue -> EmoticonResponse.from(ue.getEmoticon()))
                .collect(Collectors.toList());
    }

    @Override
    public Emoticon findEmoticonById(Long emoticonId) {
        return emoticonRepository.findById(emoticonId)
                .orElseThrow(() -> new CustomException(EmoticonErrorCode.EMOTICON_NOT_FOUND));
    }

    @Override
    @Transactional
    public EmoticonResponse purchaseEmoticon(Long userId, Long emoticonId) {
        User user = userService.findUserById(userId);
        Emoticon emoticon = findEmoticonById(emoticonId);

        if (isEmoticonOwnedByUser(userId, emoticonId)) {
            throw new CustomException(EmoticonErrorCode.ALREADY_PURCHASED);
        }

        EmoticonType type;
        int purchasePrice = 0;

        if (emoticon.isDefault()) {
            type = EmoticonType.DEFAULT;
        } else {
            type = EmoticonType.PURCHASED;
            purchasePrice = emoticon.getPrice();
            if (pointService.getCurrentBalance(userId) < purchasePrice) {
                throw new CustomException(EmoticonErrorCode.INSUFFICIENT_POINTS);
            }
            PointUseRequest request = PointUseRequest.builder()
                    .amount(emoticon.getPrice())
                    .reasonType(PointReasonType.EMOTICON_PURCHASED)
                    .entityId(emoticonId)
                    .build();
            pointService.usePoints(userId, request);
        }
        UserEmoticon userEmoticon = UserEmoticon.builder()
                .user(user)
                .emoticon(emoticon)
                .type(type)
                .purchasePrice(purchasePrice)
                .build();
        userEmoticonRepository.save(userEmoticon);
        return EmoticonResponse.from(emoticon);
    }

    @Override
    @Transactional
    public EmoticonResponse uploadEmoticon(MultipartFile file, EmoticonUploadRequest request, Long userId) throws IOException {
        fileStorageService.validateFile(file);

        FileInfo fileInfo = fileStorageService.storeFile(file, emoticonDir);
        User creator = userService.findUserById(userId);

        Emoticon emoticon = Emoticon.builder()
                .name(request.getName())
                .storedName(fileInfo.getStoredFileName())
                .imageUrl(emoticonUrlPrefix + fileInfo.getStoredFileName())
                .contentType(fileInfo.getContentType())
                .fileSize(fileInfo.getFileSize())
                .price(request.getPrice())
                .isDefault(false)
                .creator(creator)
                .build();

        Emoticon savedEmoticon = emoticonRepository.save(emoticon);
        slackNotifier.sendEmoticonUploadAlert(savedEmoticon, creator);

        return EmoticonResponse.from(savedEmoticon);
    }

    @Override
    public boolean isEmoticonOwnedByUser(Long userId, Long emoticonId) {
        return userEmoticonRepository.existsByUserIdAndEmoticonId(userId, emoticonId);
    }
}
