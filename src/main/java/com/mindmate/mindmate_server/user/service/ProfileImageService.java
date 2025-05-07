package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.user.domain.ProfileImage;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.ProfileImageResponse;
import com.mindmate.mindmate_server.user.repository.ProfileImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImageService {
    private final FileStorageService fileStorageService;
    private final ProfileImageRepository profileImageRepository;
    private final UserService userService;

    @Value("${profile.dir}")
    private String profileImageDir;

    @Value("${profile.url.prefix}")
    private String profileImageUrlPrefix;

    @Transactional
    public ProfileImageResponse uploadProfileImage(Long userId, MultipartFile file) throws IOException {

        fileStorageService.validateFile(file);
        User user = userService.findUserById(userId);

        try {
            Optional<ProfileImage> existingImage = profileImageRepository.findByUserId(userId);
            existingImage.ifPresent(image -> {
                deleteImageFile(image.getStoredName());
                profileImageRepository.delete(image);
            });

            FileInfo fileInfo = fileStorageService.storeFile(file, profileImageDir);
            String imageUrl = profileImageUrlPrefix + fileInfo.getStoredFileName();

            ProfileImage profileImage = ProfileImage.builder()
                    .user(user)
                    .originalName(fileInfo.getOriginalFileName())
                    .storedName(fileInfo.getStoredFileName())
                    .imageUrl(imageUrl)
                    .contentType("image/webp")
                    .fileSize(fileInfo.getFileSize())
                    .build();

            ProfileImage savedImage = profileImageRepository.save(profileImage);

            return ProfileImageResponse.builder()
                    .id(savedImage.getId())
                    .originalFileName(fileInfo.getOriginalFileName())
                    .storedFileName(fileInfo.getStoredFileName())
                    .imageUrl(imageUrl)
                    .contentType("image/webp")
                    .fileSize(fileInfo.getFileSize())
                    .build();

        } catch (Exception e) {
            throw new CustomException(ProfileErrorCode.IMAGE_UPLOAD_ERROR);
        }
    }

    @Transactional
    public void deleteProfileImage(Long userId, Long imageId) {
        ProfileImage profileImage = profileImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND));

        if (!profileImage.getUser().getId().equals(userId)) {
            throw new CustomException(ProfileErrorCode.PROFILE_IMAGE_ACCESS_DENIED);
        }

        deleteImageFile(profileImage.getStoredName());
        profileImageRepository.delete(profileImage);
    }

    private void deleteImageFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isEmpty()) {
            return;
        }

        File file = new File(profileImageDir, storedFileName);
        if (file.exists()) {
            file.delete();
        }
    }

    @Transactional(readOnly = true)
    public ProfileImage findProfileImageByUserId(Long userId) {
        return profileImageRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ProfileImage findProfileImageById(Long id){
        return profileImageRepository.findById(id)
                .orElseThrow(()->new CustomException(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND));
    }

    @Value("${profile.default.image.id}")
    private Long defaultProfileImageId;

    @Transactional(readOnly = true)
    public ProfileImage getDefaultProfileImage() {
        return profileImageRepository.findById(defaultProfileImageId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND));
    }
}