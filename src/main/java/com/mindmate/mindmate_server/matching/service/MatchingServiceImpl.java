package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingServiceImpl implements MatchingService{

    private final MatchingRepository matchingRepository;
    private final WaitingUserRepository waitingUserRepository;
    private final UserService userService;
    private final ChatRoomService chatRoomService;

    private final RedisMatchingService redisMatchingService;

    // 매칭 생성 -> + 채팅 생성
    @Override @Transactional
    public MatchingCreateResponse createMatching(Long userId, MatchingCreateRequest request) {
        User user = userService.findUserById(userId);

        // 활성화된 매칭 수 카운트
        int activeRoomCount = matchingRepository.countByCreatorAndStatus(user, MatchingStatus.OPEN);
        if (activeRoomCount >= 3) {
            throw new CustomException(MatchingErrorCode.MATCHING_LIMIT_EXCEED);
        }

        Matching matching = Matching.builder()
                .creator(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .categories(request.getMatchingCategories())
                .creatorRole(request.getCreatorRole())
                .anonymous(request.isAnonymous())
                .allowRandom(request.isAllowRandom())
                .showDepartment(request.isShowDepartment())
                .build();

        matchingRepository.save(matching);

        ChatRoom chatRoom = chatRoomService.createChatRoom(matching);

        matching.setChatRoom(chatRoom);

        Long matchingId = matchingRepository.save(matching).getId();

        if(request.isAllowRandom()){
            redisMatchingService.addMatchingToAvailableSet(matching);
        } // 레디스 set에 추가

        return MatchingCreateResponse.builder()
                .matchingId(matchingId)
                .chatRoomId(chatRoom.getId())
                .build();
    }

    @Override @Transactional // 수동
    public Long applyForMatching(Long userId, Long matchingId, WaitingUserRequest request) {
        User user = userService.findUserById(userId);

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        // 자기 매칭방 신청 불가
        if (matching.isCreator(user)) {
            throw new CustomException(MatchingErrorCode.CANNOT_APPLY_TO_OWN_MATCHING);
        }

        if (!matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        if (waitingUserRepository.findByMatchingAndWaitingUser(matching, user).isPresent()) {
            throw new CustomException(MatchingErrorCode.ALREADY_APPLIED_TO_MATCHING);
        }

        // 수동 매칭 신청 생성
        WaitingUser waitingUser = WaitingUser.builder()
                .waitingUser(user)
                .message(request.getMessage())
                .matchingType(MatchingType.MANUAL) // 수동 매칭
                .anonymous(request.isAnonymous())
                .build();

        matching.addWaitingUser(waitingUser);
        return waitingUserRepository.save(waitingUser).getId();
    }

    @Override @Transactional
    public Long acceptMatching(Long userId, Long matchingId, Long waitingId) {
        User creator = userService.findUserById(userId);

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        // 매칭방 소유자 확인
        if (!matching.isCreator(creator)) {
            throw new CustomException(MatchingErrorCode.NOT_MATCHING_OWNER);
        }

        if (!matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        WaitingUser waitingUser = waitingUserRepository.findById(waitingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.WAITING_NOT_FOUND));

        if (!waitingUser.getMatching().getId().equals(matchingId)) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_WAITING);
        }
        waitingUser.accept();

        // 다른 신청들은 모두 거절
        waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching).stream()
                .filter(app -> !app.getId().equals(waitingId))
                .forEach(WaitingUser::reject);

        // 매칭 완료 처리ㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇ
        matching.acceptMatching(waitingUser.getWaitingUser());

        matchingRepository.save(matching);
        return matching.getId();
    }

    @Override @Transactional
    public Long autoMatchApply(Long userId, AutoMatchingRequest request) {
        InitiatorType userRole = request.getUserRole();

        User user = userService.findUserById(userId);

        Long matchingId = redisMatchingService.getRandomMatching(userRole);

        if (matchingId == null) {
            throw new CustomException(MatchingErrorCode.NO_MATCHING_AVAILABLE);
        }

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        redisMatchingService.removeMatchingFromAvailableSet(matchingId, matching.getCreatorRole());

        // 자동 매칭 신청
        WaitingUser waitingUser = WaitingUser.builder()
                .waitingUser(user)
                .matchingType(MatchingType.AUTO_FORMAT)
                .anonymous(request.isAnonymous())
                .build();

        matching.addWaitingUser(waitingUser);
        Long waitingUserId = waitingUserRepository.save(waitingUser).getId();

        // 매칭 수락
        try {
            acceptMatching(matching.getCreator().getId(), matching.getId(), waitingUserId);
        } catch (CustomException e) {
            log.error("자동 매칭 수락 실패: {}", e.getMessage());
            throw new CustomException(MatchingErrorCode.AUTO_MATCHING_FAILED);
        }

        return matching.getChatRoom().getId();
    }

    @Override
    @Transactional
    public MatchingDetailResponse updateMatching(Long userId, Long matchingId, MatchingUpdateRequest request) {

        User user = userService.findUserById(userId);

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        if (!matching.isCreator(user)) {
            throw new CustomException(MatchingErrorCode.NOT_MATCHING_OWNER);
        }

        if (!matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        matching.updateMatchingInfo(
                request.getTitle(),
                request.getDescription(),
                request.getMatchingCategories(),
                request.isAnonymous(),
                request.isAllowRandom(),
                request.isShowDepartment()
        );

        return MatchingDetailResponse.of(matching);
    }

    @Override @Transactional
    public void cancelWaiting(Long userId, Long waitingUserId) {
        User user = userService.findUserById(userId);

        WaitingUser waitingUser = waitingUserRepository.findById(waitingUserId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.WAITING_NOT_FOUND));

        if (!waitingUser.isOwner(user)) {
            throw new CustomException(MatchingErrorCode.NOT_MATCHING_OWNER);
        }

        if (waitingUser.getStatus() != WaitingStatus.PENDING) {
            throw new CustomException(MatchingErrorCode.CANNOT_CANCEL_PROCESSED_WAITING);
        }

        if (!user.addCancelCount()) {
            throw new CustomException(MatchingErrorCode.DAILY_LIMIT_CANCEL_EXCEED);
        }

        waitingUserRepository.delete(waitingUser);
    }

    @Override
    public Page<MatchingResponse> getMatchings(Pageable pageable, MatchingCategory category,
                                               String department, InitiatorType requiredRole) {
        Page<Matching> matchings;
        MatchingStatus requireStatus = MatchingStatus.OPEN;

        if (requiredRole != null) {
            InitiatorType targetCreatorRole = requiredRole == InitiatorType.SPEAKER
                    ? InitiatorType.LISTENER
                    : InitiatorType.SPEAKER;

            matchings = matchingRepository.findByStatusAndCreatorRoleOrderByCreatedAtDesc(
                    requireStatus, targetCreatorRole, pageable);
        }
        else if (category != null && department != null) {
            matchings = matchingRepository.findByStatusAndCategoryAndDepartment(
                    requireStatus, category, department, pageable);
        }
        else if (category != null) {
            matchings = matchingRepository.findByStatusAndCategoryOrderByCreatedAtDesc(
                    requireStatus, category, pageable);
        }
        else if (department != null) {
            matchings = matchingRepository.findOpenMatchingsByDepartment(
                    requireStatus, department, pageable);
        }
        else {
            matchings = matchingRepository.findByStatusOrderByCreatedAtDesc(requireStatus, pageable);
        }

        return matchings.map(MatchingResponse::of);
    }

    @Override
    public MatchingDetailResponse getMatchingDetail(Long matchingId) {

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        return MatchingDetailResponse.of(matching);
    }

    @Override
    public Page<MatchingResponse> searchMatchings(Pageable pageable, MatchingSearchRequest request) {
        Page<Matching> matchings;
        MatchingStatus status = MatchingStatus.OPEN;
        String keyword = request.getKeyword() != null ? request.getKeyword().trim() : "";

        if (keyword.isEmpty()) {
            return getMatchings(pageable, request.getCategory(), request.getDepartment(), request.getRequiredRole());
        }

        if (request.getCategory() != null || request.getDepartment() != null || request.getRequiredRole() != null) {
            InitiatorType targetCreatorRole = null;

            if (request.getRequiredRole() != null) {
                targetCreatorRole = request.getRequiredRole() == InitiatorType.SPEAKER
                        ? InitiatorType.LISTENER
                        : InitiatorType.SPEAKER;
            }

            matchings = matchingRepository.searchByKeywordWithFilters(
                    status,
                    keyword,
                    request.getCategory(),
                    request.getDepartment(),
                    targetCreatorRole,
                    pageable
            );
        } else {
            matchings = matchingRepository.searchByKeyword(status, keyword, pageable);
        }

        return matchings.map(MatchingResponse::of);
    }

    @Override
    public List<WaitingUserResponse> getWaitingUsers(Long userId, Long matchingId) {
        User user = userService.findUserById(userId);

        Matching Matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        // 매칭방 소유자 확인
        if (!Matching.isCreator(user)) {
            throw new CustomException(MatchingErrorCode.NOT_MATCHING_OWNER);
        }

        List<WaitingUser> waitingUsers = waitingUserRepository.findByMatchingWithWaitingUserProfile(Matching);
        return waitingUsers.stream()
                .map(WaitingUserResponse::of)
                .collect(Collectors.toList());
    }

    @Override
    public Page<MatchingResponse> getUserMatchingHistory(Long userId, Pageable pageable, boolean asParticipant) {

        User user = userService.findUserById(userId);

        Page<Matching> matchings;
        if (asParticipant) {
            matchings = matchingRepository.findByAcceptedUserAndStatusOrderByMatchedAtDesc(
                    user, MatchingStatus.MATCHED, pageable);
        } else {
            matchings = matchingRepository.findByCreatorAndStatusOrderByMatchedAtDesc(
                    user, MatchingStatus.MATCHED, pageable);
        }

        return matchings.map(MatchingResponse::of);
    }

    @Override @Transactional
    public void closeMatching(Long userId, Long matchingId) {
        User user = userService.findUserById(userId);

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        if (!matching.isCreator(user)) {
            throw new CustomException(MatchingErrorCode.NOT_MATCHING_OWNER);
        }

        if (!matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        matching.closeMatching();

        redisMatchingService.removeMatchingFromAvailableSet(matchingId, matching.getCreatorRole());
        // 채팅이 끝나면 상담횟수 +1 (리스너 역할에만?)
    }

}
