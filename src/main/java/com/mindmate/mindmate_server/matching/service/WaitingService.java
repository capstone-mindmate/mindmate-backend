package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.WaitingQueue;
import com.mindmate.mindmate_server.matching.dto.ListenerStatus;
import com.mindmate.mindmate_server.matching.dto.WaitingProfile;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public interface WaitingService {

    void addToWaitingQueue(Long profileId, InitiatorType userType, Set<CounselingField> preferredFields, CounselingStyle preferredStyle);

    void cancelWaiting(Long profileId, InitiatorType userType);

    List<WaitingProfile> getWaitingUsers(InitiatorType userType);

    List<WaitingProfile> getWaitingListeners();

    List<WaitingProfile> getWaitingSpeakers();

    void updateListenerStatus(Long listenerId, ListenerStatus status);

    void updateSpeakerStatus(Long speakerId, boolean isAvailable, Set<CounselingField> preferredFields, CounselingStyle preferredStyle);

    List<WaitingQueue> findWaitingUsers(InitiatorType userType, Set<CounselingField> preferredFields, CounselingStyle preferredStyle);

    List<WaitingQueue> findWaitingSpeakers(Set<CounselingField> preferredFields, CounselingStyle preferredStyle);

    List<WaitingQueue> findWaitingListeners(Set<CounselingField> preferredFields, CounselingStyle preferredStyle);

    boolean isUserWaiting(Long profileId, InitiatorType userType);

    Set<String> getAvailableUserIds(InitiatorType userType);

    String getUserStatus(Long profileId, InitiatorType userType);
}
