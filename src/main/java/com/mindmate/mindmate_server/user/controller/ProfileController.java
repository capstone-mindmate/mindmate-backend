package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.auth.util.SecurityUtil;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "프로필", description = "스피커/리스너 프로필 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    @Operation(summary = "특정 사용자 프로필 조회", description = "특정 사용자의 프로필을 조회합니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ProfileDetailResponse> getUserProfile(@PathVariable Long userId) {
        ProfileDetailResponse profile = profileService.getProfileDetail(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "프로필 id로 프로필 상세 조회", description = "특정 프로필을 조회합니다.")
    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileDetailResponse> getProfileById(@PathVariable Long profileId) {
        ProfileDetailResponse profile = profileService.getProfileDetailById(profileId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "간소화된 프로필 조회", description = "특정 유저의 간소화된 프로필을 조회합니다.")
    @GetMapping("/users/{userId}/simple")
    public ResponseEntity<ProfileSimpleResponse> getUserSimpleProfile(@PathVariable Long userId) {
        ProfileSimpleResponse profile = profileService.getProfileSimple(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ProfileCreateRequest request) {
        Long userId = principal.getUserId();
        ProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ProfileUpdateRequest request) {
        Long userId = principal.getUserId();
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 평가태그 추가
    @PostMapping("/users/{userId}/tags")
    public ResponseEntity<Void> addEvaluationTags(
            @PathVariable Long userId,
            @RequestBody Set<String> tags) {
        profileService.addEvaluationTags(userId, tags);
        return ResponseEntity.ok().build();
    }
}
