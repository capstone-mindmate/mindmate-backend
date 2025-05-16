package com.mindmate.mindmate_server.global.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KafkaHealthDTO {
    private boolean healthy;
    private String errorMessage;
    private int brokerCount;
    private int topicCount;
    private int dltTopicCount;
    private List<BrokerInfoDTO> brokers;
    private List<ConsumerGroupInfoDTO> consumerGroups;
    private LocalDateTime timestamp = LocalDateTime.now();
}
