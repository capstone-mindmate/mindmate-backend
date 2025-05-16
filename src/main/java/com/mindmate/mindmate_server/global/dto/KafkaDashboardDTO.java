package com.mindmate.mindmate_server.global.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class KafkaDashboardDTO {
    private KafkaHealthDTO health;
    private List<DltTopicStatusDTO> dltTopics;
    private String bootstrapServers;
    private LocalDateTime timestamp;
    private String error;

}
