package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.user.dto.AdminCertificationListResponse;
import com.mindmate.mindmate_server.user.dto.AdminCertificationResponse;
import com.mindmate.mindmate_server.user.dto.CertificationProcessRequest;
import com.mindmate.mindmate_server.user.service.CertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/certifications")
public class AdminCertificationController {
    private final CertificationService certificationService;

    @GetMapping
    public ResponseEntity<AdminCertificationListResponse> getCertificationList() {
        return ResponseEntity.ok(certificationService.getCertificationList());
    }

    @GetMapping("/{listenerId}")
    public ResponseEntity<AdminCertificationResponse> getCertificationDetail(
            @PathVariable Long listenerId) {
        return ResponseEntity.ok(certificationService.getCertificationDetail(listenerId));
    }

    @PostMapping("/{listenerId}/process")
    public ResponseEntity<Void> processCertification(
            @PathVariable Long listenerId,
            @Valid @RequestBody CertificationProcessRequest request) {
        certificationService.processCertification(listenerId, request);
        return ResponseEntity.ok().build();
    }
}