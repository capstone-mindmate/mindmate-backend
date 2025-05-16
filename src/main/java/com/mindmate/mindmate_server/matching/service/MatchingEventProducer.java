package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.global.service.ResilientEventPublisher;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingEventProducer {
    private final ResilientEventPublisher eventPublisher;

    public void publishMatchingAccepted(MatchingAcceptedEvent event){
        eventPublisher.publishEvent("matching-accepted", event.getMatchingId().toString(), event);
    }
}
