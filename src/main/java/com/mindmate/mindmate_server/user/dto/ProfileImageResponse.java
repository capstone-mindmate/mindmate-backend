package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.ProfileImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResponse {
    private Long id;
    private String originalFileName;
    private String storedFileName;
    private String imageUrl;
    private String contentType;
    private long fileSize;

    public static ProfileImageResponse from(ProfileImage profileImage) {
        return ProfileImageResponse.builder()
                .id(profileImage.getId())
                .originalFileName(profileImage.getOriginalName())
                .storedFileName(profileImage.getStoredName())
                .imageUrl(profileImage.getImageUrl())
                .contentType(profileImage.getContentType())
                .fileSize(profileImage.getFileSize())
                .build();
    }
}