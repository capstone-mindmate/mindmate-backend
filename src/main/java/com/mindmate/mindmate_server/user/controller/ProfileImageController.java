package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.user.dto.ProfileImageResponse;
import com.mindmate.mindmate_server.user.service.ProfileImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/profiles/image")
@Tag(name = "Profile Image API", description = "프로필 이미지 API")
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PostMapping
    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지를 업로드합니다.")
    public ResponseEntity<ProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("file") MultipartFile file) throws IOException {
        log.info("프로필 업로드 시작@@");

        ProfileImageResponse response = profileImageService.uploadProfileImage(userPrincipal.getUserId(), file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "프로필 이미지 삭제", description = "프로필 이미지를 삭제합니다.")
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long imageId) {

        profileImageService.deleteProfileImage(userId, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/current")
    @Operation(summary = "현재 프로필 이미지 조회", description = "현재 사용자의 프로필 이미지 정보를 조회")
    public ResponseEntity<ProfileImageResponse> getCurrentProfileImage(
            @AuthenticationPrincipal Long userId) {

        try {
            ProfileImageResponse response = ProfileImageResponse.from(
                    profileImageService.findProfileImageByUserId(userId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(null);
        }
    }

    @PostMapping("/default/register")
    @Operation(summary = "기본 프로필 이미지 등록", description = "서버에 저장된 기본 이미지를 DB에 등록합니다.")
    public ResponseEntity<ProfileImageResponse> registerDefaultProfileImage() {
        ProfileImageResponse response = profileImageService.registerDefaultProfileImage();
        return ResponseEntity.ok(response);
    }

}