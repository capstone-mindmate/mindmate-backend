package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingEventProducer {

    private final KafkaTemplate<String, MatchingAcceptedEvent> kafkaTemplate;

    public void publishMatchingAccepted(MatchingAcceptedEvent event){
        kafkaTemplate.send("matching-accepted", event.getMatchingId().toString(), event);
    }
}
