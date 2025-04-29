package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.service.EmoticonService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.domain.*;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineLikeRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.notification.dto.MagazineApprovedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.MagazineRejectedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MagazineServiceImpl implements MagazineService {
    private final UserService userService;
    private final MagazineImageService magazineImageService;
    private final NotificationService notificationService;
    private final MagazinePopularityService magazinePopularityService;
    private final EmoticonService emoticonService;

    private final MagazineRepository magazineRepository;
    private final MagazineLikeRepository magazineLikeRepository;
    private final MagazineContentRepository magazineContentRepository;

    private final KafkaTemplate<String, MagazineEngagementEvent> kafkaTemplate;
    private final SlackNotifier slackNotifier;

    public final static long MAX_IMAGE_SIZE = 10;

    @Override
    @Transactional
    public MagazineResponse createMagazine(Long userId, MagazineCreateRequest request) {
        User user = userService.findUserById(userId);

        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .author(user)
                .build();

        magazine.setCategory(request.getCategory());
        magazine.setStatus(MagazineStatus.PENDING);

        Magazine savedMagazine = magazineRepository.save(magazine);
        processContents(savedMagazine, request.getContents());

        slackNotifier.sendMagazineCreateAlert(savedMagazine, user);
        return MagazineResponse.from(savedMagazine);
    }

    @Override
    @Transactional
    public MagazineResponse updateMagazine(Long magazineId, MagazineUpdateRequest request, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

        if (!magazine.getAuthor().equals(user)) {
            throw new CustomException(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
        }

        magazine.update(request.getTitle(), request.getCategory());
        
        // todo: 다 지우고 처리하는게 맞나??
        magazineContentRepository.deleteByMagazine(magazine);
        magazine.clearContents();
        processContents(magazine, request.getContents());
        
        slackNotifier.sendMagazineUpdateAlert(magazine, user);
        return MagazineResponse.from(magazine);
    }

    @Override
    @Transactional
    public void deleteMagazine(Long magazineId, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

//        if (!magazine.getAuthor().equals(user) && !user.getCurrentRole().equals(RoleType.ROLE_ADMIN)) {
//            throw new CustomException(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
//        }

        if (!user.getCurrentRole().equals(RoleType.ROLE_ADMIN)) {
            throw new CustomException(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
        }

        for (MagazineContent content : magazine.getContents()) {
            if (content.getType() == MagazineContentType.IMAGE && content.getImage() != null) {
                magazineImageService.deleteImage(content.getImage().getStoredName());
            }
        }

        magazinePopularityService.removePopularityScores(magazineId, magazine.getCategory());
        magazineRepository.delete(magazine);
    }

    @Override
    public Page<MagazineResponse> getMagazines(Long userId, MagazineSearchFilter filter, Pageable pageable) {
        return magazineRepository.findMagazinesWithFilters(filter, pageable);
    }

    @Override
    public MagazineDetailResponse getMagazine(Long magazineId, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

        // PUBLISHED 아닌 매거진 작성자만 확인 가능
        if (!magazine.getAuthor().equals(user) &&
                magazine.getMagazineStatus() != MagazineStatus.PUBLISHED) {
            throw new CustomException(MagazineErrorCode.MAGAZINE_NOT_FOUND);
        }

        // todo: 동일 ipAddress view 차단
        magazinePopularityService.incrementViewCount(magazine, userId);

        boolean isAuthor = magazine.getAuthor().equals(user);
        boolean isLiked = magazineLikeRepository.existsByMagazineAndUser(magazine, user);

        return MagazineDetailResponse.from(magazine, isAuthor, isLiked);

    }

    @Override
    public Magazine findMagazineById(Long magazineId) {
        return magazineRepository.findById(magazineId)
                .orElseThrow(() -> new CustomException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
    }

    @Override
    @Transactional
    public MagazineResponse manageMagazine(Long magazineId, boolean isAccepted) {
        Magazine magazine = findMagazineById(magazineId);

        if (magazine.getMagazineStatus().equals(MagazineStatus.PUBLISHED)) {
            throw new CustomException(MagazineErrorCode.ALREADY_PUBLISHED);
        }

        if (isAccepted) {
            magazine.setStatus(MagazineStatus.PUBLISHED);

            magazinePopularityService.initializePopularityScore(magazine);

            MagazineApprovedNotificationEvent authorEvent = MagazineApprovedNotificationEvent.builder()
                    .recipientId(magazine.getAuthor().getId())
                    .magazineId(magazineId)
                    .magazineTitle(magazine.getTitle())
                    .build();

            notificationService.processNotification(authorEvent);
        } else {
            magazine.setStatus(MagazineStatus.REJECTED);
            // todo: 거절 시 추가 동작

            MagazineRejectedNotificationEvent event = MagazineRejectedNotificationEvent.builder()
                    .recipientId(magazine.getAuthor().getId())
                    .magazineId(magazineId)
                    .magazineTitle(magazine.getTitle())
                    .rejectionReason("관리자 검토 결과 부적합") // 추후에 더 정확한 피드백 줘도 될듯
                    .build();

            notificationService.processNotification(event);
        }
        return MagazineResponse.from(magazineRepository.save(magazine));
    }

    @Override
    public Page<MagazineResponse> getPendingMagazines(Pageable pageable) {
        Page<Magazine> pendingMagazines = magazineRepository.findByMagazineStatus(MagazineStatus.PENDING, pageable);
        return pendingMagazines.map(MagazineResponse::from);
    }

    @Override
    @Transactional
    public LikeResponse toggleLike(Long magazineId, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

        boolean isLiked = magazineLikeRepository.existsByMagazineAndUser(magazine, user);

        if (isLiked) {
            magazineLikeRepository.deleteByMagazineAndUser(magazine, user);
            magazine.removeLike(user);

            magazinePopularityService.updateLikeScore(magazine, false);
            return LikeResponse.of(false, magazine.getLikeCount());
        } else {
            MagazineLike like = MagazineLike.builder()
                    .magazine(magazine)
                    .user(user)
                    .build();
            magazineLikeRepository.save(like);
            magazine.addLike(user);

            magazinePopularityService.updateLikeScore(magazine, true);
            return LikeResponse.of(true, magazine.getLikeCount());
        }
    }

    @Override
    public void handleEngagement(Long userId, Long magazineId, MagazineEngagementRequest request) {
        MagazineEngagementEvent event = MagazineEngagementEvent.builder()
                .userId(userId)
                .magazineId(magazineId)
                .dwellTime(request.getDwellTime())
                .scrollPercentage(request.getScrollPercentage())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send("magazine-engagement-topic", event);
    }

    @Override
    public List<MagazineResponse> getPopularMagazines(int limit) {
        return magazinePopularityService.getPopularMagazines(limit);
    }

    @Override
    public List<MagazineResponse> getPopularMagazinesByCategory(MatchingCategory category, int limit) {
        return magazinePopularityService.getPopularMagazinesByCategory(category, limit);
    }
    
    private void processContents(Magazine magazine, List<MagazineContentDTO> contents) {
        int order = 0;
        for (MagazineContentDTO dto : contents) {
            MagazineContent content;
            
            switch (dto.getType()) {
                case TEXT:
                    if (StringUtils.isBlank(dto.getText())) {
                        continue;
                    }
                    content = MagazineContent.builder()
                            .magazine(magazine)
                            .type(MagazineContentType.TEXT)
                            .text(dto.getText())
                            .contentOrder(order++)
                            .build();
                    break;

                case IMAGE:
                    if (dto.getImageId() == null) {
                        continue;
                    }
                    MagazineImage image = magazineImageService.findMagazineImageById(dto.getImageId());
                    Optional<MagazineContent> usedContent = magazineContentRepository
                            .findByImageAndType(image, MagazineContentType.IMAGE);

                    if (usedContent.isPresent() && !usedContent.get().getMagazine().equals(magazine)) {
                        throw new CustomException(MagazineErrorCode.MAGAZINE_IMAGE_ALREADY_IN_USE);
                    }

                    content = MagazineContent.builder()
                            .magazine(magazine)
                            .type(MagazineContentType.IMAGE)
                            .image(image)
                            .contentOrder(order++)
                            .build();
                    break;

                case EMOTICON:
                    if (dto.getEmoticonId() == null) {
                        continue;
                    }
                    Emoticon emoticon = emoticonService.findEmoticonById(dto.getEmoticonId());

                    content = MagazineContent.builder()
                            .magazine(magazine)
                            .type(MagazineContentType.EMOTICON)
                            .emoticon(emoticon)
                            .contentOrder(order++)
                            .build();
                    break;

                default:
                    continue;
            }
            magazine.addContent(content);
            magazineContentRepository.save(content);
        }
    }

}
