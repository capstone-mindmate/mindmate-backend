package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.repository.UserRepository;
import com.mindmate.mindmate_server.user.service.ProfileService;
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

    // 매칭 생성 -> + 채팅 생성
    @Override @Transactional
    public Long createMatching(Long userId, MatchingCreateRequest request) {
        User user = userService.findUserById(userId);

        // 활성화된 매칭 수 카운트
        int activeRoomCount = matchingRepository.countByCreatorAndStatus(user, MatchingStatus.OPEN);
        if (activeRoomCount >= 3) {
            throw new CustomException(MatchingErrorCode.MATCHING_LIMIT_EXCEED);
        }

        // 채팅방 생성
        User speaker = request.getCreatorRole() == InitiatorType.SPEAKER ? user : null;
        User listener = request.getCreatorRole() == InitiatorType.LISTENER ? user : null;

        ChatRoom chatRoom = chatRoomService.createChatRoom(speaker, listener, request.getTitle());

        Matching matching = Matching.builder()
                .creator(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getMatchingCategory())
                .creatorRole(request.getCreatorRole())
                .chatRoom(chatRoom)
                .build();

        return matchingRepository.save(matching).getId();
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
                .build();

        matching.addWaitingUser(waitingUser);
        return waitingUserRepository.save(waitingUser).getId();
    }

    @Override @Transactional
    public Long acceptMatching(Long userId, Long matchingId, Long waitingUserId) {
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

        WaitingUser waitingUser = waitingUserRepository.findById(waitingUserId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.WAITING_NOT_FOUND));

        // 현재 매칭방인지 확인
        if (!waitingUser.getMatching().getId().equals(matchingId)) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_WAITING);
        }

        User applicant = waitingUser.getWaitingUser();

        waitingUser.accept();

        // 다른 신청들은 모두 거절
        waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching).stream()
                .filter(app -> !app.getId().equals(waitingUserId))
                .forEach(WaitingUser::reject);

        // 채팅방 업데이트 - 상대방 추가ㅇㅇ 근데 todo : 채팅 로직 확인해야됨
        User speaker = matching.isCreatorSpeaker() ? creator : applicant;
        User listener = matching.isCreatorSpeaker() ? applicant : creator;

        chatRoomService.updateChatRoomParticipant(matching.getChatRoom().getId(), speaker, listener);

        // 매칭 완료 처리ㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇ
        matching.acceptMatching(applicant, matching.getChatRoom());

        // 채팅이 끝나면 상담횟수 +1 (리스너 역할에만?)

        return matching.getId();
    }

    @Override @Transactional
    public Long autoMatchApply(Long userId, AutoMatchingRequest request) {
        InitiatorType userRole = request.getUserRole();
        MatchingCategory category = request.getCategory();
        String department = request.getDepartment();
        String message = request.getMessage();

        User user = userService.findUserById(userId);

        // 신청한 role과 반대되는 걸로
        InitiatorType targetCreatorRole = userRole == InitiatorType.SPEAKER
                ? InitiatorType.LISTENER
                : InitiatorType.SPEAKER;

        List<Matching> candidateRooms;

        // 학과 필터링이 있는 경우
        if (department != null && !department.isEmpty()) {
            candidateRooms = matchingRepository
                    .findByStatusRoleCategoryAndDepartment(
                            MatchingStatus.OPEN, targetCreatorRole, category, department);

            if (candidateRooms.isEmpty()) {
                log.info("같은 학과 매칭 없음.. 전체 학과로 검색");
                candidateRooms = matchingRepository
                        .findByStatusAndCreatorRoleAndCategoryOrderByCreatedAt(
                                MatchingStatus.OPEN, targetCreatorRole, category);
            }
        } else { // 없을 때
            candidateRooms = matchingRepository
                    .findByStatusAndCreatorRoleAndCategoryOrderByCreatedAt(
                            MatchingStatus.OPEN, targetCreatorRole, category);
        }

        if (candidateRooms.isEmpty()) {
            throw new CustomException(MatchingErrorCode.NO_MATCHING_AVAILABLE);
        }

        // 제일 오래된걸로. (나중에 알고리즘 수정해야할듯)
        Matching matching = candidateRooms.get(0);

        // 자동 신청됨
        WaitingUser waitingUser = WaitingUser.builder()
                .waitingUser(user)
                .message(message)
                .matchingType(MatchingType.AUTO_FORMAT)
                .build();

        matching.addWaitingUser(waitingUser);
        waitingUser = waitingUserRepository.save(waitingUser);

        // 매칭 수락
        try {
            acceptMatching(matching.getCreator().getId(), matching.getId(), waitingUser.getId());
        } catch (CustomException e) {
            // 자동 매칭 실패 -> 이후에 어떻게 처리?
            log.error("자동 매칭 수락 처리 실패: {}", e.getMessage());
            throw new CustomException(MatchingErrorCode.AUTO_MATCHING_FAILED);
        }

        return waitingUser.getId();
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

        // 필터링 로직 (카테고리, 학과, 원하는 역할 등) -> 이후에 바꾸거나 해야할듯
        if (requiredRole != null) {
            matchings = matchingRepository.findByStatusAndCreatorRoleOrderByCreatedAtDesc(
                    MatchingStatus.OPEN,
                    requiredRole == InitiatorType.SPEAKER ? InitiatorType.LISTENER : InitiatorType.SPEAKER,
                    pageable);
        } else if (category != null && department != null) {
            matchings = matchingRepository.findByStatusAndCategoryAndDepartment(
                    MatchingStatus.OPEN, category, department, pageable);
        } else if (category != null) {
            matchings = matchingRepository.findByStatusAndCategoryOrderByCreatedAtDesc(
                    MatchingStatus.OPEN, category, pageable);
        } else if (department != null) {
            matchings = matchingRepository.findOpenMatchingsByDepartment(department, pageable);
        } else {
            matchings = matchingRepository.findByStatusOrderByCreatedAtDesc(MatchingStatus.OPEN, pageable);
        }

        // todo : 필터링 로직 더 추가
        return matchings.map(MatchingResponse::of);
    }

    @Override
    public MatchingResponse getMatchingDetail(Long matchingId) {

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        return MatchingResponse.of(matching);
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
    public Page<Matching> getUserMatchingHistory(Long userId, Pageable pageable, boolean asParticipant) {

        User user = userService.findUserById(userId);

        if (asParticipant) {
            return matchingRepository.findByAcceptedUserAndStatusOrderByMatchedAtDesc(
                    user, MatchingStatus.MATCHED, pageable);
        } else {
            return matchingRepository.findByCreatorAndStatusOrderByMatchedAtDesc(
                    user, MatchingStatus.MATCHED, pageable);
        }
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
    }

}
