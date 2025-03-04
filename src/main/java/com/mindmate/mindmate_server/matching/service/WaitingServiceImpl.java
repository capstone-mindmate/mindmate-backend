package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.WaitingQueue;
import com.mindmate.mindmate_server.matching.dto.ListenerStatus;
import com.mindmate.mindmate_server.matching.dto.WaitingProfile;
import com.mindmate.mindmate_server.matching.repository.WaitingQueueRepository;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import com.mindmate.mindmate_server.user.repository.ListenerRepository;
import com.mindmate.mindmate_server.user.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingServiceImpl implements WaitingService { // 대기상태 관리

    private final WaitingQueueRepository waitingQueueRepository;
    private final ListenerRepository listenerProfileRepository;
    private final SpeakerRepository speakerProfileRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String USERS_AVAILABLE = "users:available:";
    private static final String LISTENERS_AVAILABLE = USERS_AVAILABLE + "listeners";
    private static final String SPEAKERS_AVAILABLE = USERS_AVAILABLE + "speakers";
    private static final String USER_STATUS_PREFIX = "user:status:";

    @Transactional
    @Override
    public void addToWaitingQueue(Long profileId, InitiatorType userType,
                                  Set<CounselingField> preferredFields, CounselingStyle preferredStyle) {

        Optional<WaitingQueue> existingQueue;
        if (userType == InitiatorType.SPEAKER) {
            existingQueue = waitingQueueRepository.findBySpeakerProfileIdAndActiveTrue(profileId);
        } else {
            existingQueue = waitingQueueRepository.findByListenerProfileIdAndActiveTrue(profileId);
        }

        if (existingQueue.isPresent()) { // 이미 대기중일 경우
            WaitingQueue queue = existingQueue.get();
            queue.updatePreferences(preferredFields, preferredStyle);
            waitingQueueRepository.save(queue);
            return;
        }

        // 아닐 시 대기
        WaitingQueue waitingQueue;
        if (userType == InitiatorType.SPEAKER) {
            SpeakerProfile speakerProfile = speakerProfileRepository.findById(profileId)
                    .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

            waitingQueue = WaitingQueue.builder()
                    .speakerProfile(speakerProfile)
                    .waitingType(InitiatorType.SPEAKER)
                    .preferredFields(preferredFields)
                    .preferredStyle(preferredStyle)
                    .build();
        } else {
            ListenerProfile listenerProfile = listenerProfileRepository.findById(profileId)
                    .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

            waitingQueue = WaitingQueue.builder()
                    .listenerProfile(listenerProfile)
                    .waitingType(InitiatorType.LISTENER)
                    .preferredFields(preferredFields)
                    .preferredStyle(preferredStyle)
                    .build();
        }

        waitingQueueRepository.save(waitingQueue);

    }

    @Transactional
    @Override
    public void cancelWaiting(Long profileId, InitiatorType userType) {
        Optional<WaitingQueue> cancelQueue;

        if (userType == InitiatorType.SPEAKER) {
            cancelQueue = waitingQueueRepository.findBySpeakerProfileIdAndActiveTrue(profileId);
        } else {
            cancelQueue = waitingQueueRepository.findByListenerProfileIdAndActiveTrue(profileId);
        }

        if (cancelQueue.isPresent()) {
            WaitingQueue waitingQueue = cancelQueue.get();
            waitingQueue.deactivate();
            waitingQueueRepository.save(waitingQueue);

        }
    }

    // 대기중인 유저
    @Transactional(readOnly = true)
    @Override
    public List<WaitingProfile> getWaitingUsers(InitiatorType userType) {
        if (userType == InitiatorType.LISTENER) {
            return getWaitingListeners();
        } else {
            return getWaitingSpeakers();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<WaitingProfile> getWaitingListeners() {
        List<WaitingQueue> waitingListeners = waitingQueueRepository.findByWaitingTypeAndActiveOrderByCreatedAtAsc(
                InitiatorType.LISTENER, true);

        return waitingListeners.stream()
                .map(wq -> {
                    ListenerProfile listener = wq.getListenerProfile();

                    return WaitingProfile.builder()
                            .profileId(listener.getId())
                            .nickname(listener.getNickname())
                            .profileImage(listener.getProfileImage())
                            .requestedFields(listener.getCounselingFields().stream()
                                    .map(field -> field.getField().getTitle())
                                    .collect(Collectors.toSet()))
                            .preferredStyle(listener.getCounselingStyle().getTitle())
                            .waitingSince(wq.getCreatedAt())
                            .userType("LISTENER")
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<WaitingProfile> getWaitingSpeakers() {
        List<WaitingQueue> waitingSpeakers = waitingQueueRepository.findByWaitingTypeAndActiveOrderByCreatedAtAsc(
                InitiatorType.SPEAKER, true);

        return waitingSpeakers.stream()
                .map(wq -> {
                    SpeakerProfile speaker = wq.getSpeakerProfile();

                    return WaitingProfile.builder()
                            .profileId(speaker.getId())
                            .nickname(speaker.getNickname())
                            .profileImage(speaker.getProfileImage())
                            .requestedFields(wq.getPreferredFields().stream()
                                    .map(CounselingField::getTitle)
                                    .collect(Collectors.toSet()))
                            .preferredStyle(wq.getPreferredStyle() != null ? wq.getPreferredStyle().getTitle() : null)
                            .waitingSince(wq.getCreatedAt())
                            .userType("SPEAKER")
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 상태 업데이트
    @Transactional
    @Override
    public void updateListenerStatus(Long listenerId, ListenerStatus status) {
        ListenerProfile listener = listenerProfileRepository.findById(listenerId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        // 레디스에 저장
        redisTemplate.opsForValue().set(USER_STATUS_PREFIX + "listener:" + listenerId, status.name());

        if (status == ListenerStatus.AVAILABLE) {
            redisTemplate.opsForSet().add(LISTENERS_AVAILABLE, listenerId.toString());

            Set<CounselingField> fields = listener.getCounselingFields().stream()
                    .map(cf -> cf.getField())
                    .collect(Collectors.toSet());

            addToWaitingQueue(listenerId, InitiatorType.LISTENER, fields, listener.getCounselingStyle());
        } else {
            redisTemplate.opsForSet().remove(LISTENERS_AVAILABLE, listenerId.toString());

            cancelWaiting(listenerId, InitiatorType.LISTENER);
        }
    }

    @Transactional
    @Override
    public void updateSpeakerStatus(Long speakerId, boolean isAvailable,
                                    Set<CounselingField> preferredFields, CounselingStyle preferredStyle) {

        String status = isAvailable ? "WAITING" : "INACTIVE";
        // 레디스에 저장
        redisTemplate.opsForValue().set(USER_STATUS_PREFIX + "speaker:" + speakerId, status);

        if (isAvailable) {
            redisTemplate.opsForSet().add(SPEAKERS_AVAILABLE, speakerId.toString());

            addToWaitingQueue(speakerId, InitiatorType.SPEAKER, preferredFields, preferredStyle);
        } else {
            redisTemplate.opsForSet().remove(SPEAKERS_AVAILABLE, speakerId.toString());

            cancelWaiting(speakerId, InitiatorType.SPEAKER);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<WaitingQueue> findWaitingUsers(InitiatorType userType,
                                               Set<CounselingField> preferredFields,
                                               CounselingStyle preferredStyle) {
        if (userType == InitiatorType.SPEAKER) {
            return findWaitingSpeakers(preferredFields, preferredStyle);
        } else {
            return findWaitingListeners(preferredFields, preferredStyle);
        }
    }

    // 조건에 맞는 사용자 찾기 todo : querydsl로 단순화
    @Transactional(readOnly = true)
    @Override
    public List<WaitingQueue> findWaitingSpeakers(Set<CounselingField> preferredFields, CounselingStyle preferredStyle) {
        if (preferredFields != null && !preferredFields.isEmpty() && preferredStyle != null) {
            return waitingQueueRepository.findActiveSpeakersByFieldsAndStyle(preferredFields, preferredStyle);
        } else if (preferredFields != null && !preferredFields.isEmpty()) {
            return waitingQueueRepository.findActiveSpeakersByPreferredFields(preferredFields);
        } else if (preferredStyle != null) {
            return waitingQueueRepository.findByWaitingTypeAndActiveAndPreferredStyleOrderByCreatedAtAsc(
                    InitiatorType.SPEAKER, true, preferredStyle);
        } else {
            return waitingQueueRepository.findByWaitingTypeAndActiveOrderByCreatedAtAsc(
                    InitiatorType.SPEAKER, true);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<WaitingQueue> findWaitingListeners(Set<CounselingField> preferredFields, CounselingStyle preferredStyle) {
        if (preferredFields != null && !preferredFields.isEmpty() && preferredStyle != null) {
            return waitingQueueRepository.findActiveListenersByFieldsAndStyle(preferredFields, preferredStyle);
        } else if (preferredFields != null && !preferredFields.isEmpty()) {
            return waitingQueueRepository.findActiveListenersByPreferredFields(preferredFields);
        } else if (preferredStyle != null) {
            return waitingQueueRepository.findByWaitingTypeAndActiveAndPreferredStyleOrderByCreatedAtAsc(
                    InitiatorType.LISTENER, true, preferredStyle);
        } else {
            return waitingQueueRepository.findByWaitingTypeAndActiveOrderByCreatedAtAsc(
                    InitiatorType.LISTENER, true);
        }
    }

    // 사용자 대기 중인지 확인
    @Transactional(readOnly = true)
    @Override
    public boolean isUserWaiting(Long profileId, InitiatorType userType) {
        if (userType == InitiatorType.SPEAKER) {
            return waitingQueueRepository.findBySpeakerProfileIdAndActiveTrue(profileId).isPresent();
        } else {
            return waitingQueueRepository.findByListenerProfileIdAndActiveTrue(profileId).isPresent();
        }
    }


    // 레디스 - 상담 가능한 유저
    @Override
    public Set<String> getAvailableUserIds(InitiatorType userType) {
        String redisKey = userType == InitiatorType.SPEAKER ? SPEAKERS_AVAILABLE : LISTENERS_AVAILABLE;
        return redisTemplate.opsForSet().members(redisKey);
    }

    // 레디스 - 사용자 상태
    @Override
    public String getUserStatus(Long profileId, InitiatorType userType) {
        String userTypeStr = userType == InitiatorType.SPEAKER ? "speaker" : "listener";
        return redisTemplate.opsForValue().get(USER_STATUS_PREFIX + userTypeStr + ":" + profileId);
    }

}
