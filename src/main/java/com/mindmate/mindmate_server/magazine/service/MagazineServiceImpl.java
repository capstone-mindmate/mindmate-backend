package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.domain.MagazineLike;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.repository.MagazineImageRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineLikeRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.notification.dto.MagazineApprovedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.MagazineRejectedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MagazineServiceImpl implements MagazineService {
    private final UserService userService;
    private final MagazineImageService magazineImageService;
    private final NotificationService notificationService;
    private final MagazinePopularityService magazinePopularityService;

    private final MagazineRepository magazineRepository;
    private final MagazineLikeRepository magazineLikeRepository;
    private final MagazineImageRepository magazineImageRepository;

    private final KafkaTemplate<String, MagazineEngagementEvent> kafkaTemplate;
    private final SlackNotifier slackNotifier;

    public final static long MAX_IMAGE_SIZE = 10;

    @Override
    @Transactional
    public MagazineResponse createMagazine(Long userId, MagazineCreateRequest request) {
        User user = userService.findUserById(userId);

        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(user)
                .build();

        magazine.setCategory(request.getCategory());
        magazine.setStatus(MagazineStatus.PENDING);

        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            if (request.getImageIds().size() > MAX_IMAGE_SIZE) {
                throw new CustomException(MagazineErrorCode.TOO_MANY_IMAGES);
            }

            List<MagazineImage> images = magazineImageRepository.findAllById(request.getImageIds());

            // 이미지가 다른 매거진에 연결되어 있는지 확인
            for (MagazineImage image : images) {
                if (image.getMagazine() != null) {
                    throw new CustomException(MagazineErrorCode.MAGAZINE_IMAGE_ALREADY_IN_USE);
                }
                magazine.addImage(image);
            }
        }

        Magazine savedMagazine = magazineRepository.save(magazine);
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

        magazine.update(request.getTitle(), request.getContent(), request.getCategory());

        // 해당 이미지가 다른 곳에 연결되어 있는 경우
        if (request.getImageIds() != null) {
            List<MagazineImage> existingImages = new ArrayList<>(magazine.getImages());
            Set<Long> newImageIds = new HashSet<>(request.getImageIds());

            // 기존 존재 이미지가 포함되지 않은 경우 -> 삭제
            for (MagazineImage image : existingImages) {
                if (!newImageIds.contains(image.getId())) {
                    magazine.removeImage(image);
                    magazineImageService.deleteImage(image.getStoredName());
                    magazineImageRepository.delete(image);
                }
            }

            // 새 이미지 연결
            List<MagazineImage> newImages = magazineImageRepository.findAllById(request.getImageIds());
            for (MagazineImage image : newImages) {
                if (image.getMagazine() != null && !image.getMagazine().equals(magazine)) {
                    throw new CustomException(MagazineErrorCode.MAGAZINE_IMAGE_ALREADY_IN_USE);
                }

                if (image.getMagazine() == null) {
                    magazine.addImage(image);
                }
            }
        }

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

        for (MagazineImage image : magazine.getImages()) {
            magazineImageService.deleteImage(image.getStoredName());
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

}
