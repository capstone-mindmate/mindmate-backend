package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.matching.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface MatchingService {

    MatchingCreateResponse createMatching(Long userId, MatchingCreateRequest request);

    Long applyForMatching(Long userId, Long matchingId, WaitingUserRequest request);

    Long acceptMatching(Long userId, Long matchingId, Long waitingId);

    Long autoMatchApply(Long userId, AutoMatchingRequest request);

    MatchingDetailResponse updateMatching(Long userId, Long matchingId, MatchingUpdateRequest request);

    void cancelWaiting(Long userId, Long waitingUserId);

    Page<MatchingResponse> getMatchings(Long userId, Pageable pageable, MatchingCategory category,
                                                        String department, InitiatorType requiredRole);

    MatchingDetailResponse getMatchingDetail(Long matchingId);

    Page<MatchingResponse> searchMatchings(Long UserId, Pageable pageable, MatchingSearchRequest request);

    Page<WaitingUserResponse> getWaitingUsers(Long userId, Long matchingId, Pageable pageable);

    Page<MatchingResponse> getCreatedMatchings(Long userId, Pageable pageable);

    Page<AppliedMatchingResponse> getAppliedMatchings(Long userId, Pageable pageable);

    void cancelMatching(Long userId, Long matchingId);

    Matching findMatchingById(Long matchingId);

    Map<String, Integer> getCategoryCountsByUserId(Long userId);

    MatchingStatusResponse getMatchingStatus(Long userId);

    PopularMatchingCategoryResponse getPopularMatchingCategoty();
}
