package com.mindmate.mindmate_server.global.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileInfo {
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private long fileSize;
}
