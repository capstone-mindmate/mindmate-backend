package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MatchingService {

    MatchingCreateResponse createMatching(Long userId, MatchingCreateRequest request);

    Long applyForMatching(Long userId, Long matchingId, WaitingUserRequest request);

    Long acceptMatching(Long userId, Long matchingId, Long waitingId);

    Long autoMatchApply(Long userId, AutoMatchingRequest request);

    MatchingDetailResponse updateMatching(Long userId, Long matchingId, MatchingUpdateRequest request);

    void cancelWaiting(Long userId, Long waitingUserId);

    Page<MatchingResponse> getMatchings(Pageable pageable, MatchingCategory category,
                                        String department, InitiatorType requiredRole);

    MatchingDetailResponse getMatchingDetail(Long matchingId);

    Page<MatchingResponse> searchMatchings(Pageable pageable, MatchingSearchRequest request);

    List<WaitingUserResponse> getWaitingUsers(Long userId, Long matchingId);

    Page<MatchingResponse> getUserMatchingHistory(Long userId, Pageable pageable, boolean asParticipant);

    void closeMatching(Long userId, Long matchingId);

    Matching findMatchingById(Long matchingId);
}
