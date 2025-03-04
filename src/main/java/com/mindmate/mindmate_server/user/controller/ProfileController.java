package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.service.ProfileService;
import com.mindmate.mindmate_server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "프로필", description = "스피커/리스너 프로필 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final UserService userService;

    @Operation(summary = "리스너 프로필 조회", description = "리스너의 프로필을 조회합니다.")
    @GetMapping("/listener/{profileId}")
    public ResponseEntity<ListenerProfileResponse> getListenerProfile(@PathVariable Long profileId) {
        return ResponseEntity.ok(profileService.getListenerProfile(profileId));
    }

    @Operation(summary = "스피커 프로필 조회", description = "스피커의 프로필을 조회합니다.")
    @GetMapping("/speaker/{profileId}")
    public ResponseEntity<SpeakerProfileResponse> getSpeakerProfile(@PathVariable Long profileId) {
        return ResponseEntity.ok(profileService.getSpeakerProfile(profileId));
    }

    @Operation(summary = "리스너 프로필 생성", description = "리스너 프로필을 생성합니다.")
    @PostMapping("/listener")
    public ResponseEntity<ProfileResponse> createListenerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ListenerProfileRequest request) {
        ProfileResponse response = profileService.createListenerProfile(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "스피커 프로필 생성", description = "스피커 프로필을 생성합니다.")
    @PostMapping("/speaker")
    public ResponseEntity<ProfileResponse> createSpeakerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SpeakerProfileRequest request) {
        ProfileResponse response = profileService.createSpeakerProfile(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "리스너 프로필 수정", description = "리스너의 프로필을 수정합니다.")
    @PutMapping("/listener")
    public ResponseEntity<ListenerProfileResponse> updateListenerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ListenerProfileUpdateRequest request) {
        User user = userService.findUserById(principal.getUserId());
        return ResponseEntity.ok(profileService.updateListenerProfile(user.getListenerProfile().getId(), request));
    }

    @Operation(summary = "스피커 프로필 수정", description = "스피커 프로필을 수정합니다.")
    @PutMapping("/speaker")
    public ResponseEntity<SpeakerProfileResponse> updateSpeakerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SpeakerProfileUpdateRequest request) {
        User user = userService.findUserById(principal.getUserId());
        return ResponseEntity.ok(profileService.updateSpeakerProfile(user.getSpeakerProfile().getId(), request));
    }

    /* 이후에 여러 자격을 가질 때를 고려해야함*/
    @Operation(summary = "리스너 자격 수정", description = "리스너의 자격을 수정합니다.")
    @PutMapping("/listener/certification")
    public ResponseEntity<ListenerProfileResponse> updateListenerCertification(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CertificationUpdateRequest request) {
        User user = userService.findUserById(principal.getUserId());
        return ResponseEntity.ok(profileService.updateListenerCertification(user.getListenerProfile().getId(), request));
    }

    @Operation(summary = "역할 전환", description = "리스너/스피커 역할을 전환합니다.")
    @PostMapping("/switch-role")
    public ResponseEntity<?> switchRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam RoleType targetRole) {
        ProfileStatusResponse response = profileService.switchRole(principal.getUserId(), targetRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "상담 스타일 목록 조회", description = "가능한 상담 스타일 목록을 조회합니다.")
    @GetMapping("/counseling-styles")
    public ResponseEntity<List<CounselingResponse>> getCounselingStyles() {
        return ResponseEntity.ok(Arrays.stream(CounselingStyle.values())
                .map(style -> new CounselingResponse(style.getTitle()))
                .collect(Collectors.toList()));
    }

    @Operation(summary = "상담 분야 목록 조회", description = "가능한 상담 분야 목록을 조회합니다.")
    @GetMapping("/counseling-fields")
    public ResponseEntity<List<CounselingResponse>> getCounselingField() {
        return ResponseEntity.ok(Arrays.stream(CounselingField.values())
                .map(field -> new CounselingResponse(field.getTitle()))
                .collect(Collectors.toList()));
    }
}
