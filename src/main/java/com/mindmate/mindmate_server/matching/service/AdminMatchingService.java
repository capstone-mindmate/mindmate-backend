package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.matching.domain.WaitingStatus;
import com.mindmate.mindmate_server.matching.dto.MatchingResponse;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminMatchingService {

    private final MatchingRepository matchingRepository;
    private final MatchingService matchingService;
    private final ChatRoomService chatRoomService;
    private final RedisMatchingService redisMatchingService;
    private final WaitingUserRepository waitingUserRepository;

    public Page<MatchingResponse> getMatchings(Pageable pageable, MatchingCategory category) {

        Page<Matching> matchings = matchingRepository.getMatchingsWithFilters(
                MatchingStatus.OPEN,
                category,
                null,
                null,
                null,
                null,
                pageable
        );

        return matchings.map(MatchingResponse::of);
    }

    @Transactional
    public void rejectMatching(Long matchingId){

        Matching matching = matchingService.findMatchingById(matchingId);

        if (!matching.isOpen()) {
            throw new CustomException(MatchingErrorCode.MATCHING_ALREADY_CLOSED);
        }

        List<Long> waitingUserIds = waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching)
                .stream()
                .filter(app -> app.getStatus() == WaitingStatus.PENDING)
                .map(app -> {
                    app.reject();
                    return app.getWaitingUser().getId();
                })
                .collect(Collectors.toList());

        chatRoomService.deleteChatRoom(matching.getChatRoom());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisMatchingService.decrementUserActiveMatchingCount(matching.getCreator().getId());
                redisMatchingService.removeMatchingFromAvailableSet(matchingId, matching.getCreatorRole());

                redisMatchingService.cleanupMatchingKeys(matching);

                for (Long waitingUserId : waitingUserIds) {
                    redisMatchingService.decrementUserActiveMatchingCount(waitingUserId);
                }
            }
        });

        matching.rejectMatching();
    }
}
