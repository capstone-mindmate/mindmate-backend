package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    @PostMapping("/listener")
    public ResponseEntity<ProfileResponse> createListenerProfile(@Valid @RequestBody ListenerProfileRequest request) {
        ProfileResponse response = profileService.createListenerProfile(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/speaker")
    public ResponseEntity<ProfileResponse> createSpeakerProfile(@Valid @RequestBody SpeakerProfileRequest request) {
        ProfileResponse response = profileService.createSpeakerProfile(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch-role")
    public ResponseEntity<?> switchRole(@RequestParam RoleType targetRole) {
        ProfileStatusResponse response = profileService.switchRole(targetRole);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/counseling-styles")
    public ResponseEntity<List<CounselingResponse>> getCounselingStyles() {
        return ResponseEntity.ok(Arrays.stream(CounselingStyle.values())
                .map(style -> new CounselingResponse(style.getTitle()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/counseling-fields")
    public ResponseEntity<List<CounselingResponse>> getCounselingField() {
        return ResponseEntity.ok(Arrays.stream(CounselingField.values())
                .map(field -> new CounselingResponse(field.getTitle()))
                .collect(Collectors.toList()));
    }

}
