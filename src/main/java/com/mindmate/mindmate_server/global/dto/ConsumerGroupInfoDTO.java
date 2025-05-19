package com.mindmate.mindmate_server.global.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConsumerGroupInfoDTO {
    private String groupId;
    private String status;
    private boolean isHealthy;
    private boolean isSimple;
    private String errorMessage;
    private int partitionCount;
    private List<String> subscribedTopics;
}
