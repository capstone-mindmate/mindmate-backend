package com.mindmate.mindmate_server.global.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrokerInfoDTO {
    private int id;
    private String host;
    private int port;
    private String rack;
    private boolean isController;
}