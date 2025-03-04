package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.dto.MatchingResponse;
import com.mindmate.mindmate_server.matching.dto.WaitingProfile;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public interface MatchingService {

    MatchingResponse autoRandomMatch(Long profileId, InitiatorType initiatorType);

    MatchingResponse autoFormatMatch(Long profileId, InitiatorType initiatorType,
                                     Set<CounselingField> requestedFields,
                                     CounselingStyle preferredStyle);

    MatchingResponse manualMatch(Long initiatorId, InitiatorType initiatorType,
                                 Long recipientId, Set<CounselingField> requestedFields);

    MatchingResponse acceptMatching(Long matchingId, Long profileId, InitiatorType profileType);

    MatchingResponse rejectMatching(Long matchingId, Long profileId, InitiatorType profileType, String reason);

    MatchingResponse cancelMatching(Long matchingId, Long profileId, InitiatorType profileType);

    MatchingResponse completeMatching(Long matchingId, Long profileId, InitiatorType profileType);

    List<WaitingProfile> getWaitingListenersForMatching();

    List<WaitingProfile> getWaitingSpeakersForMatching();
}
