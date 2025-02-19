package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.ListenerProfileRequest;
import com.mindmate.mindmate_server.user.dto.ProfileResponse;
import com.mindmate.mindmate_server.user.dto.ProfileStatusResponse;
import com.mindmate.mindmate_server.user.dto.SpeakerProfileRequest;
import com.mindmate.mindmate_server.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
