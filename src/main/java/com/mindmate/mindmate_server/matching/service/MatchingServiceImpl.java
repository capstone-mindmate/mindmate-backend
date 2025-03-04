package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.ListenerStatus;
import com.mindmate.mindmate_server.matching.dto.MatchingResponse;
import com.mindmate.mindmate_server.matching.dto.WaitingProfile;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.user.domain.*;
import com.mindmate.mindmate_server.user.repository.ListenerRepository;
import com.mindmate.mindmate_server.user.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingServiceImpl implements MatchingService {

    private final MatchingRepository matchingRepository;
    private final SpeakerRepository speakerRepository;
    private final ListenerRepository listenerRepository;
    private final WaitingService waitingService;
    private final KafkaTemplate<String, String> kafkaTemplate; // string으로 충분할지 고민해보기

    // 랜덤 매칭
    @Transactional
    @Override
    public MatchingResponse autoRandomMatch(Long profileId, InitiatorType initiatorType) {
        if (initiatorType == InitiatorType.SPEAKER) {
            return handleSpeakerRandomMatch(profileId);
        } else {
            return handleListenerRandomMatch(profileId);
        }
    }

    private MatchingResponse handleListenerRandomMatch(Long listenerProfileId) {
        ListenerProfile listenerProfile = listenerRepository.findById(listenerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        List<WaitingQueue> waitingSpeakers = waitingService.findWaitingUsers(InitiatorType.SPEAKER, null, null);

        if (waitingSpeakers.isEmpty()) { // 없을 때만 추가
            waitingService.updateListenerStatus(listenerProfileId, ListenerStatus.AVAILABLE);

            return MatchingResponse.builder()
                    .status(MatchingStatus.REQUESTED)
                    .message("상담 가능한 스피커가 없습니다. 대기열에 추가됩니다.")
                    .build();
        }

        WaitingQueue waitingSpeaker = waitingSpeakers.get(0);
        SpeakerProfile speakerProfile = waitingSpeaker.getSpeakerProfile();

        Matching matching = Matching.builder()
                .speakerProfile(speakerProfile)
                .listenerProfile(listenerProfile)
                .type(MatchingType.AUTO_RANDOM)
                .initiator(InitiatorType.LISTENER)
                .requestedFields(waitingSpeaker.getPreferredFields())
                .build();

        Matching matched = matchingRepository.save(matching);

        waitingService.updateSpeakerStatus(speakerProfile.getId(), false, null, null);

        publishMatchingEvent("MATCHING_REQUESTED", matched);

        return convertToMatchingResponse(matched);
    }

    private MatchingResponse handleSpeakerRandomMatch(Long speakerProfileId) {
        SpeakerProfile speakerProfile = speakerRepository.findById(speakerProfileId)
                .orElseThrow(()-> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        Set<String> availableListenerIds = waitingService.getAvailableUserIds(InitiatorType.LISTENER);
        if (availableListenerIds == null || availableListenerIds.isEmpty()) {
            waitingService.updateSpeakerStatus(speakerProfileId, true, null, null);

            return MatchingResponse.builder()
                    .status(MatchingStatus.REQUESTED)
                    .message("상담 가능한 리스너가 없습니다. 대기열에 추가됩니다.")
                    .build();
        }

        String randomListenerId = getRandomElement(availableListenerIds);
        ListenerProfile listenerProfile = listenerRepository.findById(Long.parseLong(randomListenerId))
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        Matching matching = Matching.builder()
                .speakerProfile(speakerProfile)
                .listenerProfile(listenerProfile)
                .type(MatchingType.AUTO_RANDOM)
                .initiator(InitiatorType.SPEAKER)
                .build();

        Matching matched = matchingRepository.save(matching);

        publishMatchingEvent("MATCHING_REQUESTED", matched);

        return convertToMatchingResponse(matched);
    }

    // 형식 매칭
    @Transactional
    @Override
    public MatchingResponse autoFormatMatch(Long profileId, InitiatorType initiatorType,
                                               Set<CounselingField> requestedFields,
                                               CounselingStyle preferredStyle) {
        if (initiatorType == InitiatorType.SPEAKER) {
            return handleSpeakerFormatMatch(profileId, requestedFields, preferredStyle);
        } else {
            return handleListenerFormatMatch(profileId, requestedFields, preferredStyle);
        }
    }

    private MatchingResponse handleSpeakerFormatMatch(Long speakerProfileId,
                                                         Set<CounselingField> requestedFields,
                                                         CounselingStyle preferredStyle) {

        SpeakerProfile speakerProfile = speakerRepository.findById(speakerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        List<WaitingQueue> availableListenerQueues = waitingService.findWaitingListeners(requestedFields, preferredStyle);

        if (availableListenerQueues.isEmpty()) { // 조건에 안 맞을 시
            waitingService.addToWaitingQueue(speakerProfileId, InitiatorType.SPEAKER, requestedFields, preferredStyle);

            return MatchingResponse.builder()
                    .status(MatchingStatus.REQUESTED)
                    .message("해당 분야의 리스너가 없습니다. 대기열에 추가됩니다.")
                    .build();
        }

        ListenerProfile bestListener = availableListenerQueues.get(0).getListenerProfile();

        Matching matching = Matching.builder()
                .speakerProfile(speakerProfile)
                .listenerProfile(bestListener)
                .type(MatchingType.AUTO_FORMAT)
                .requestedFields(requestedFields)
                .initiator(InitiatorType.SPEAKER)
                .build();

        Matching matched = matchingRepository.save(matching);

        waitingService.cancelWaiting(bestListener.getId(), InitiatorType.LISTENER);

        publishMatchingEvent("MATCHING_REQUESTED", matched);

        return convertToMatchingResponse(matched);
    }

    private MatchingResponse handleListenerFormatMatch(Long listenerProfileId,
                                                          Set<CounselingField> preferredFields,
                                                          CounselingStyle preferredStyle) {

        ListenerProfile listenerProfile = listenerRepository.findById(listenerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        List<WaitingQueue> waitingSpeakers = waitingService.findWaitingSpeakers(preferredFields, preferredStyle);

        if (waitingSpeakers.isEmpty()) {
            waitingService.addToWaitingQueue(listenerProfileId, InitiatorType.LISTENER, preferredFields, preferredStyle);

            return MatchingResponse.builder()
                    .status(MatchingStatus.REQUESTED)
                    .message("해당 분야의 스피커가 없습니다. 대기열에 추가됩니다.")
                    .build();
        }

        WaitingQueue waitingSpeaker = waitingSpeakers.get(0);
        SpeakerProfile speakerProfile = waitingSpeaker.getSpeakerProfile();

        Matching matching = Matching.builder()
                .speakerProfile(speakerProfile)
                .listenerProfile(listenerProfile)
                .type(MatchingType.AUTO_FORMAT)
                .requestedFields(waitingSpeaker.getPreferredFields())
                .initiator(InitiatorType.LISTENER)
                .build();

        Matching matched = matchingRepository.save(matching);

        waitingService.cancelWaiting(speakerProfile.getId(), InitiatorType.SPEAKER);

        publishMatchingEvent("MATCHING_REQUESTED", matched);

        return convertToMatchingResponse(matched);
    }

    // 수동 매칭 - 직접 선택
    @Transactional
    @Override
    public MatchingResponse manualMatch(Long initiatorId, InitiatorType initiatorType,
                                           Long recipientId, Set<CounselingField> requestedFields) {
        if (initiatorType == InitiatorType.SPEAKER) {
            return manualMatchBySpeaker(initiatorId, recipientId, requestedFields);
        } else {
            return manualMatchByListener(initiatorId, recipientId);
        }
    }

    private MatchingResponse manualMatchBySpeaker(Long speakerProfileId, Long listenerProfileId,
                                                     Set<CounselingField> requestedFields) {

        SpeakerProfile speakerProfile = speakerRepository.findById(speakerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        ListenerProfile listenerProfile = listenerRepository.findById(listenerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        boolean isListenerAvailable = waitingService.isUserWaiting(listenerProfileId, InitiatorType.LISTENER);
        if (!isListenerAvailable) {
            throw new CustomException(MatchingErrorCode.USER_NOT_AVAILABLE);
        }

        Matching matching = Matching.builder()
                .speakerProfile(speakerProfile)
                .listenerProfile(listenerProfile)
                .type(MatchingType.MANUAL)
                .requestedFields(requestedFields)
                .initiator(InitiatorType.SPEAKER)
                .build();

        Matching savedMatching = matchingRepository.save(matching);

        publishMatchingEvent("MATCHING_REQUESTED", savedMatching);

        return convertToMatchingResponse(savedMatching);
    }

    private MatchingResponse manualMatchByListener(Long listenerProfileId, Long speakerProfileId) {

        ListenerProfile listenerProfile = listenerRepository.findById(listenerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        SpeakerProfile speakerProfile = speakerRepository.findById(speakerProfileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        boolean isSpeakerWaiting = waitingService.isUserWaiting(speakerProfileId, InitiatorType.SPEAKER);

        if (!isSpeakerWaiting) {
            throw new CustomException(MatchingErrorCode.USER_NOT_AVAILABLE);
        }

        Matching matching = Matching.builder()
                .speakerProfile(speakerProfile)
                .listenerProfile(listenerProfile)
                .type(MatchingType.MANUAL)
                .requestedFields(null) // 대기 큐에서 선호 필드 정보를 가져올까..?
                .initiator(InitiatorType.LISTENER)
                .build();

        Matching savedMatching = matchingRepository.save(matching);

        waitingService.cancelWaiting(speakerProfile.getId(), InitiatorType.SPEAKER);

        publishMatchingEvent("MATCHING_REQUESTED", savedMatching);

        return convertToMatchingResponse(savedMatching);
    }

    // 수락
    @Transactional
    @Override
    public MatchingResponse acceptMatching(Long matchingId, Long profileId, InitiatorType profileType) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        if (matching.getStatus() != MatchingStatus.REQUESTED) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_STATUS);
        } // 엔티티 안에 넣을까..?

        //권한 확인
        validateRecipientAuthorization(matching, profileId, profileType);

        matching.accept();

        // 채팅방 아이디... 생성??? 연결...
//        matching.setChatRoomId(chatRoomId);

        ListenerProfile listener = matching.getListenerProfile();
        waitingService.updateListenerStatus(listener.getId(), ListenerStatus.BUSY);

        publishMatchingEvent("MATCHING_ACCEPTED", matching);

        return convertToMatchingResponse(matching);
    }

    // 거절
    @Transactional
    @Override
    public MatchingResponse rejectMatching(Long matchingId, Long profileId, InitiatorType profileType, String reason) {

        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        if (matching.getStatus() != MatchingStatus.REQUESTED) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_STATUS);
        }

        // 권한 확인
        validateRecipientAuthorization(matching, profileId, profileType);

        // 제한 확인
        User user;
        if (profileType == InitiatorType.LISTENER) {
            user = matching.getListenerProfile().getUser();
        } else {
            user = matching.getSpeakerProfile().getUser();
        }

        if (!user.addRejectionCount()) {
            throw new CustomException(MatchingErrorCode.LIMIT_EXCEED);
        }

        matching.reject(reason);

        publishMatchingEvent("MATCHING_REJECTED", matching);

        return convertToMatchingResponse(matching);
    }

    @Transactional
    @Override
    public MatchingResponse cancelMatching(Long matchingId, Long profileId, InitiatorType profileType) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        if (matching.getStatus() != MatchingStatus.REQUESTED && matching.getStatus() != MatchingStatus.ACCEPTED) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_STATUS);
        }

        validateInitiatorAuthorization(matching, profileId, profileType);

        // 제한 확인
        User user;
        if (profileType == InitiatorType.LISTENER) {
            user = matching.getListenerProfile().getUser();
        } else {
            user = matching.getSpeakerProfile().getUser();
        }

        if (!user.addCancelCount()) {
            throw new CustomException(MatchingErrorCode.LIMIT_EXCEED);
        }

        matching.cancel();

        // 수락되었을경우
        if (matching.getStatus() == MatchingStatus.ACCEPTED) {
            waitingService.updateListenerStatus(matching.getListenerProfile().getId(), ListenerStatus.AVAILABLE);
        }

        publishMatchingEvent("MATCHING_CANCELED", matching);

        return convertToMatchingResponse(matching);
    }

    // 매칭 성사됨
    @Transactional
    @Override
    public MatchingResponse completeMatching(Long matchingId, Long profileId, InitiatorType profileType) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

        if (matching.getStatus() != MatchingStatus.ACCEPTED) {
            throw new CustomException(MatchingErrorCode.INVALID_MATCHING_STATUS);
        }

        // 권한 - 리스너 스피커 상관 ㄴ
        boolean isAuthorized = (profileType == InitiatorType.SPEAKER && matching.getSpeakerProfile().getId().equals(profileId)) ||
                (profileType == InitiatorType.LISTENER && matching.getListenerProfile().getId().equals(profileId));

        if (!isAuthorized) {
            throw new CustomException(MatchingErrorCode.USER_NOT_AUTHORIZED);
        }

        matching.complete();

        ListenerProfile listener = matching.getListenerProfile();
        SpeakerProfile speaker = matching.getSpeakerProfile();

        listener.incrementCounselingCount();
        speaker.incrementCounselingCount();

        waitingService.updateListenerStatus(listener.getId(), ListenerStatus.AVAILABLE);

        publishMatchingEvent("MATCHING_COMPLETED", matching);

        return convertToMatchingResponse(matching);
    }

    // 매칭 이력 조회? 필요하나? 프로필에 보여줄건가
    // 대기중인 사용자 보여줘?
    // 진행중인 매칭 조회?

    private String getRandomElement(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }

        int randomIndex = (int) (Math.random() * set.size());
        int i = 0;
        for (String element : set) {
            if (i == randomIndex) {
                return element;
            }
            i++;
        }
        return null;
    }

    // 요청 받은 사람 권한
    private void validateRecipientAuthorization(Matching matching, Long profileId, InitiatorType profileType) {
        boolean isAuthorized = false;

        if (matching.getInitiator() == InitiatorType.SPEAKER &&
                profileType == InitiatorType.LISTENER &&
                matching.getListenerProfile().getId().equals(profileId)) {
            isAuthorized = true;
        }

        if (matching.getInitiator() == InitiatorType.LISTENER &&
                profileType == InitiatorType.SPEAKER &&
                matching.getSpeakerProfile().getId().equals(profileId)) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            throw new CustomException(MatchingErrorCode.USER_NOT_AUTHORIZED);
        }
    }

    // 요청 보낸 사람 - 당사자만 취소 가능
    private void validateInitiatorAuthorization(Matching matching, Long profileId, InitiatorType profileType) {
        boolean isAuthorized = false;

        if (matching.getInitiator() == InitiatorType.SPEAKER &&
                profileType == InitiatorType.SPEAKER &&
                matching.getSpeakerProfile().getId().equals(profileId)) {
            isAuthorized = true;
        }

        if (matching.getInitiator() == InitiatorType.LISTENER &&
                profileType == InitiatorType.LISTENER &&
                matching.getListenerProfile().getId().equals(profileId)) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            throw new CustomException(MatchingErrorCode.USER_NOT_AUTHORIZED);
        }
    }

    private MatchingResponse convertToMatchingResponse(Matching matching) {
        return MatchingResponse.builder()
                .id(matching.getId())
                .speakerProfileId(matching.getSpeakerProfile().getId())
                .listenerProfileId(matching.getListenerProfile().getId())
                .status(matching.getStatus())
                .type(matching.getType())
                .initiator(matching.getInitiator())
                .requestedFields(matching.getRequestedFields().stream()
                        .map(CounselingField::getTitle)
                        .collect(Collectors.toSet()))
                .requestedAt(matching.getCreatedAt())
                .matchedAt(matching.getMatchedAt())
                .completedAt(matching.getCompletedAt())
                .chatRoomId(matching.getChatRoomId())
                .build();
    }

    private void publishMatchingEvent(String eventType, Matching matching) {
        String message = String.format("%s:%d:%d:%d",
                eventType,
                matching.getId(),
                matching.getSpeakerProfile().getId(),
                matching.getListenerProfile().getId()
        );

        kafkaTemplate.send("matching-events", message);
        log.info("Published matching event: {}", message);
    }

    @Override
    public List<WaitingProfile> getWaitingListenersForMatching() {
        return waitingService.getWaitingListeners();
    }

    @Override
    public List<WaitingProfile> getWaitingSpeakersForMatching() {
        return waitingService.getWaitingSpeakers();
    }

}
