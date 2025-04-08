package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.magazine.dto.ImageResponse;
import com.mindmate.mindmate_server.magazine.service.MagazineImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/magazine/image")
public class MagazineImageController {
    private final MagazineImageService magazineImageService;

    @PostMapping
    public ResponseEntity<List<ImageResponse>> uploadImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<ImageResponse> responses = magazineImageService.uploadImages(files);
            return ResponseEntity.ok(responses);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long imageId) {
        magazineImageService.deleteImageById(principal.getUserId(), imageId);
        return ResponseEntity.noContent().build();
    }
}
