package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.MatchingCreateRequest;
import com.mindmate.mindmate_server.matching.dto.MatchingResponse;
import com.mindmate.mindmate_server.matching.dto.WaitingUserResponse;
import com.mindmate.mindmate_server.user.repository.UserRepository;
import com.mindmate.mindmate_server.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingServiceImpl implements MatchingService {

    private MatchingRepository matchingRepository;
    private WaitinUserRepository waitinUserRepository;
    private UserRepository userRepository;
    private ProfileService profileService;
    private ChatRoomService chatRoomService;

    // 매칭 생성 -> + 채팅 생성
    @Transactional
    public Long creatematching(Long userId, MatchingCreateRequest request) {
    }

    @Transactional
    public void findMatchAndAccept(Matching matching) {

    }

    @Transactional
    public Long applyForMatching(Long userId, Long matchingId, WaitingUsesrRequest request) {

    }

    @Transactional
    public Long acceptMatching(Long userId, Long matchingId, Long applicationId) {

    }

    @Transactional
    public void cancelWaiting(Long userId, Long applicationId) {

    }

    public Page<MatchingResponse> getmatchings(Pageable pageable, MatchingCategory category,
                                                       String department, MatchingRoleType requiredRole) {

    }

    public MatchingResponse getmatchingDetail(Long matchingId) {

    }

    public List<WaitingUserResponse> getMatchingApplications(Long userId, Long matchingId) {

    }

    public Page<Matching> getUserMatchingHistory(Long userId, Pageable pageable, boolean asParticipant) {
    }

    @Transactional
    public void closematching(Long userId, Long matchingId) {

    }

}
