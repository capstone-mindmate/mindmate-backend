package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.global.exception.PointErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.notification.dto.MatchingAcceptedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.MatchingAppliedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointRequest;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MatchingServiceImpl implements MatchingService {

    private final MatchingRepository matchingRepository;
    private final WaitingUserRepository waitingUserRepository;

    private final UserService userService;
    private final ChatRoomService chatRoomService;
    private final RedisMatchingService redisMatchingService;
    private final NotificationService notificationService;
    private final PointService pointService;
    private final MatchingEventProducer matchingEventProducer;

    private static final int  MAX_ACTIVE_MATCHINGS = 3;

    @Override @Transactional
    public MatchingCreateResponse createMatching(Long userId, MatchingCreateRequest request) {
        User user = userService.findUserById(userId);

        validateActiveMatchingCount(userId);

        Matching matching = Matching.builder()
                .creator(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .creatorRole(request.getCreatorRole())
                .anonymous(request.isAnonymous())
                .allowRandom(request.isAllowRandom())
                .showDepartment(request.isShowDepartment())
                .build();

        Matching saved = matchingRepository.save(matching);

        ChatRoom chatRoom = chatRoomService.createChatRoom(matching);

        matching.setChatRoom(chatRoom);

        Long matchingId = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if(request.isAllowRandom()){
                    redisMatchingService.addMatchingToAvailableSet(saved);
                }
                redisMatchingService.incrementUserActiveMatchingCount(userId);
            }
        });

        return MatchingCreateResponse.builder()
                .matchingId(matchingId)
                .chatRoomId(chatRoom.getId())
                .build();
    }

    @Override @Transactional
    public Long applyForMatching(Long userId, Long matchingId, WaitingUserRequest request) {
        User user = userService.findUserById(userId);
        Matching matching = findMatchingById(matchingId);

        validateActiveMatchingCount(userId);
        validateMatchingApplication(user, matching);

        WaitingUser waitingUser = createWaitingUser(user, matching, request);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisMatchingService.incrementUserActiveMatchingCount(userId);

                sendMatchingAppliedNotification(user, matching, request.isAnonymous());
            }
        });

        return waitingUserRepository.save(waitingUser).getId();
    }

    @Override @Transactional
    public Long acceptMatching(Long userId, Long matchingId, Long waitingId) {
        Matching matching = validateMatchingOwnership(userId, matchingId, true);
        WaitingUser waitingUser = validateWaitingUser(waitingId, matchingId);

        User speakerUser;
        if (matching.getCreatorRole() == InitiatorType.SPEAKER) {
            speakerUser = matching.getCreator();
        } else {
            speakerUser = waitingUser.getWaitingUser();
        }

        try {
            pointService.usePoints(speakerUser.getId(), PointRequest.builder()
                    .transactionType(TransactionType.SPEND)
                    .amount(100)
                    .reasonType(PointReasonType.COUNSELING_REQUESTED)
                    .entityId(matchingId)
                    .build());
        } catch (CustomException e) {
            if (e.getErrorCode() == PointErrorCode.INSUFFICIENT_POINTS) {
                throw new CustomException(MatchingErrorCode.INSUFFICIENT_POINTS_FOR_MATCHING);
            }
            throw e;
        }

        waitingUser.accept();
        matching.acceptMatching(waitingUser.getWaitingUser());
        matchingRepository.save(matching);

        List<Long> pendingWaitingUserIds = findPendingWaitingUserIds(matching, waitingId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisMatchingService.cleanupMatchingKeys(matching);

                sendMatchingAcceptedNotification(matching, waitingUser);

                if (!pendingWaitingUserIds.isEmpty()) {
                    matchingEventProducer.publishMatchingAccepted(
                            MatchingAcceptedEvent.builder()
                                    .matchingId(matching.getId())
                                    .creatorId(matching.getCreator().getId())
                                    .acceptedUserId(waitingUser.getWaitingUser().getId())
                                    .pendingWaitingUserIds(pendingWaitingUserIds)
                                    .build()
                    );
                }
            }
        });

        matching.getChatRoom().updateChatRoomStatus(ChatRoomStatus.ACTIVE);
        return matching.getId();
    }

    @Override @Transactional
    public Long autoMatchApply(Long userId, AutoMatchingRequest request) {
        InitiatorType userRole = request.getUserRole();
        User user = userService.findUserById(userId);

        validateActiveMatchingCount(userId);

        Long matchingId = redisMatchingService.getRandomMatching(user, userRole);
        if (matchingId == null) {
            throw new CustomException(MatchingErrorCode.NO_MATCHING_AVAILABLE);
        }

        Matching matching = findMatchingById(matchingId);

        WaitingUser waitingUser = WaitingUser.builder()
                .waitingUser(user)
                .matchingType(MatchingType.AUTO_RANDOM)
                .anonymous(request.isAnonymous())
                .build();

        matching.addWaitingUser(waitingUser);
        Long waitingUserId = waitingUserRepository.save(waitingUser).getId();

        try {
            acceptMatching(matching.getCreator().getId(), matching.getId(), waitingUserId);
        } catch (CustomException e) {
            log.error("자동 매칭 수락 실패: {}", e.getMessage());
            throw new CustomException(MatchingErrorCode.AUTO_MATCHING_FAILED);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisMatchingService.removeMatchingFromAvailableSet(matchingId, matching.getCreatorRole());
            }
        });

        return matching.getChatRoom().getId();
    }

    @Override
    @Transactional
    public MatchingDetailResponse updateMatching(Long userId, Long matchingId, MatchingUpdateRequest request) {

        Matching matching = validateMatchingOwnership(userId, matchingId, true);

        matching.updateMatchingInfo(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.isAnonymous(),
                request.isAllowRandom(),
                request.isShowDepartment()
        );

        return MatchingDetailResponse.of(matching);
    }

    @Override @Transactional
    public void cancelWaiting(Long userId, Long waitingUserId) {
        WaitingUser waitingUser = findWaitingUserById(waitingUserId);

        waitingUserRepository.delete(waitingUser);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisMatchingService.decrementUserActiveMatchingCount(userId);
            }
        });
    }

    @Override
    public Page<MatchingResponse> getMatchings(Long userId, Pageable pageable, MatchingCategory category,
                                               String department, InitiatorType requiredRole) {
        List<Long> appliedMatchingIds = waitingUserRepository.findMatchingIdsByWaitingUserId(userId);

        if (appliedMatchingIds.isEmpty()) {
            appliedMatchingIds = Collections.singletonList(-1L);
        }

        InitiatorType targetCreatorRole = null;
        if (requiredRole != null) {
            targetCreatorRole = requiredRole == InitiatorType.SPEAKER
                    ? InitiatorType.LISTENER
                    : InitiatorType.SPEAKER;
        }

        Page<Matching> matchings = matchingRepository.getMatchingsWithFilters(
                MatchingStatus.OPEN,
                category,
                department,
                targetCreatorRole,
                userId,
                appliedMatchingIds,
                pageable
        );

        return matchings.map(MatchingResponse::of);
    }

    @Override
    public MatchingDetailResponse getMatchingDetail(Long matchingId) {
        Matching matching = findMatchingById(matchingId);
        return MatchingDetailResponse.of(matching);
    }

    @Override
    public Page<MatchingResponse> searchMatchings(Long userId, Pageable pageable, MatchingSearchRequest request) {

        List<Long> appliedMatchingIds = waitingUserRepository.findMatchingIdsByWaitingUserId(userId);

        if (appliedMatchingIds.isEmpty()) {
            appliedMatchingIds = Collections.singletonList(-1L);
        }

        InitiatorType targetCreatorRole = null;
        if (request.getRequiredRole() != null) {
            targetCreatorRole = request.getRequiredRole() == InitiatorType.SPEAKER
                    ? InitiatorType.LISTENER
                    : InitiatorType.SPEAKER;
        }

        Page<Matching> matchings = matchingRepository.searchMatchingsWithFilters(
                MatchingStatus.OPEN,
                request.getKeyword(),
                request.getCategory(),
                request.getDepartment(),
                request.getRequiredRole() != null
                        ? (request.getRequiredRole() == InitiatorType.SPEAKER ? InitiatorType.LISTENER : InitiatorType.SPEAKER)
                        : null,
                userId,
                appliedMatchingIds,
                pageable
        );

        return matchings.map(MatchingResponse::of);
    }

    @Override
    public Page<WaitingUserResponse> getWaitingUsers(Long userId, Long matchingId, Pageable pageable) {
        Matching matching = validateMatchingOwnership(userId, matchingId, false);
        if(matching.getStatus()!=MatchingStatus.OPEN){
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_STATUS);
        }
        Page<WaitingUser> waitingUsers = waitingUserRepository.findByMatchingWithWaitingUserProfile(matching, pageable);
        return waitingUsers.map(WaitingUserResponse::of);
    }

    @Override
    public Page<MatchingResponse> getCreatedMatchings(Long userId, Pageable pageable) {
        Page<Matching> matchings = matchingRepository.findByCreatorIdAndStatusOrderByCreatedAtDesc(userId, MatchingStatus.OPEN, pageable);
        return matchings.map(MatchingResponse::of);
    }

    @Override
    public Page<AppliedMatchingResponse> getAppliedMatchings(Long userId,  Pageable pageable) {
        Page<WaitingUser> waitingUsers = waitingUserRepository.findByWaitingUserIdAndMatchingStatusOrderByCreatedAtDesc(
                userId, MatchingStatus.OPEN, pageable);

        return waitingUsers.map(waitingUser ->
                AppliedMatchingResponse.of(waitingUser.getMatching(), waitingUser));
    }

    @Override @Transactional
    public void cancelMatching(Long userId, Long matchingId) {
        Matching matching = validateMatchingOwnership(userId, matchingId, true);

        List<Long> waitingUserIds = waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching)
                .stream()
                .filter(app -> app.getStatus() == WaitingStatus.PENDING)
                .map(app -> {
                    app.reject();
                    return app.getWaitingUser().getId();
                })
                .collect(Collectors.toList());

        chatRoomService.deleteChatRoom(matching.getChatRoom());
        matching.cancelMatching();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisMatchingService.decrementUserActiveMatchingCount(userId);
                redisMatchingService.removeMatchingFromAvailableSet(matchingId, matching.getCreatorRole());

                redisMatchingService.cleanupMatchingKeys(matching);

                for (Long waitingUserId : waitingUserIds) {
                    redisMatchingService.decrementUserActiveMatchingCount(waitingUserId);
                }
            }
        });
    }

    @Override
    public Matching findMatchingById(Long matchingId) {
        return matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));
    }

    @Override
    public Map<String, Integer> getCategoryCountsByUserId(Long userId) {
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (MatchingCategory category : MatchingCategory.values()) {
            categoryCounts.put(category.name(), 0);
        }

        List<Object[]> matchingCounts = matchingRepository.countMatchingsByUserAndCategory(userId);
        for (Object[] result : matchingCounts) {
            MatchingCategory category = (MatchingCategory) result[0];
            Integer count = ((Long) result[1]).intValue();
            categoryCounts.put(category.name(), count);
        }

        return categoryCounts;
    }

    @Override
    public MatchingStatusResponse getMatchingStatus(Long userId) {
                int currentCount = redisMatchingService.getUserActiveMatchingCount(userId);

                        return MatchingStatusResponse.builder()
                                .currentActiveMatchings(currentCount)
                                .maxActiveMatchings(MAX_ACTIVE_MATCHINGS)
                                .canCreateMore(currentCount < MAX_ACTIVE_MATCHINGS)
                                .build();
    }

    private WaitingUser findWaitingUserById(Long waitingUserId) {
        return waitingUserRepository.findById(waitingUserId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.WAITING_NOT_FOUND));
    }

    private void validateActiveMatchingCount(Long userId) {
        int activeRoomCount = redisMatchingService.getUserActiveMatchingCount(userId);
        if (activeRoomCount >= MAX_ACTIVE_MATCHINGS) {
            throw new CustomException(MatchingErrorCode.MATCHING_LIMIT_EXCEED);
        }
    }

    private void validateMatchingApplication(User user, Matching matching) {
        if (matching.isCreator(user)) {
            throw new CustomException(MatchingErrorCode.CANNOT_APPLY_TO_OWN_MATCHING);
        }

        if (!matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        if (waitingUserRepository.findByMatchingAndWaitingUser(matching, user).isPresent()) {
            throw new CustomException(MatchingErrorCode.ALREADY_APPLIED_TO_MATCHING);
        }
    }

    private WaitingUser createWaitingUser(User user, Matching matching, WaitingUserRequest request) {
        WaitingUser waitingUser = WaitingUser.builder()
                .waitingUser(user)
                .message(request.getMessage())
                .matchingType(MatchingType.MANUAL)
                .anonymous(request.isAnonymous())
                .build();

        matching.addWaitingUser(waitingUser);
        return waitingUser;
    }

    private void sendMatchingAppliedNotification(User user, Matching matching, boolean anonymous) {
        String applicantName = user.getProfile() != null ?
                (anonymous ? "익명" : user.getProfile().getNickname()) : "사용자";

        MatchingAppliedNotificationEvent event = MatchingAppliedNotificationEvent.builder()
                .recipientId(matching.getCreator().getId())
                .matchingId(matching.getId())
                .matchingTitle(matching.getTitle())
                .applicantNickname(applicantName)
                .build();

        notificationService.processNotification(event);
    }

    private void sendMatchingAcceptedNotification(Matching matching, WaitingUser waitingUser) {
        MatchingAcceptedNotificationEvent event = MatchingAcceptedNotificationEvent.builder()
                .recipientId(waitingUser.getWaitingUser().getId())
                .matchingId(matching.getId())
                .matchingTitle(matching.getTitle())
                .build();

        notificationService.processNotification(event);
    }

    private List<Long> findPendingWaitingUserIds(Matching matching, Long acceptedWaitingId) {
        return waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching)
                .stream()
                .filter(app -> !app.getId().equals(acceptedWaitingId) && app.getStatus() == WaitingStatus.PENDING)
                .map(WaitingUser::getId)
                .collect(Collectors.toList());
    }

    private Matching validateMatchingOwnership(Long userId, Long matchingId, boolean checkOpen) {
        User user = userService.findUserById(userId);
        Matching matching = findMatchingById(matchingId);

        if (!matching.isCreator(user)) {
            throw new CustomException(MatchingErrorCode.NOT_MATCHING_OWNER);
        }

        if (checkOpen && !matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        return matching;
    }

    private WaitingUser validateWaitingUser(Long waitingId, Long matchingId) {
        WaitingUser waitingUser = waitingUserRepository.findById(waitingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.WAITING_NOT_FOUND));

        if (!waitingUser.getMatching().getId().equals(matchingId)) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_WAITING);
        }

        return waitingUser;
    }


}
