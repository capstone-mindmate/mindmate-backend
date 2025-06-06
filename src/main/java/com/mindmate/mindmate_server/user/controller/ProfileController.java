package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "프로필", description = "프로필 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class ProfileController {
    private final ProfileService profileService;

    @Operation(summary = "본인 프로필 조회", description = "자신의 프로필을 조회합니다.")
    @GetMapping
    public ResponseEntity<ProfileDetailResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileDetailResponse profile = profileService.getMyProfileDetail(principal.getUserId());
        return ResponseEntity.ok(profile);
    }

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

    @Operation(summary = "프로필 생성", description = "새로운 사용자 프로필을 생성합니다.")
    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ProfileCreateRequest request) {
        Long userId = principal.getUserId();
        ProfileResponse response = profileService.createProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 수정", description = "기존 사용자 프로필 정보를 수정합니다.")
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ProfileUpdateRequest request) {
        Long userId = principal.getUserId();
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "관리자용 전체 프로필 조회", description = "관리자가 전체 프로필을 조회합니다.")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProfileDetailResponse>> getAllProfiles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProfileDetailResponse> profiles = profileService.getAllProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }
}
