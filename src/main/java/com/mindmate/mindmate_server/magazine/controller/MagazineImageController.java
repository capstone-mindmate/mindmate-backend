package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.magazine.dto.ImageResponse;
import com.mindmate.mindmate_server.magazine.service.MagazineImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(
        name = "매거진 이미지",
        description = "매거진 작성 시 이미지 업로드 및 삭제를 위한 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/magazine/image")
public class MagazineImageController {
    private final MagazineImageService magazineImageService;

    @Operation(
            summary = "매거진 이미지 업로드",
            description = """
                매거진 작성/수정 시 여러 이미지를 업로드합니다.
                - 최대 10개까지 한 번에 업로드할 수 있습니다.
                - 업로드 성공 시 이미지 URL과 ID 목록을 반환합니다.
            """
    )
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

    @Operation(
            summary = "매거진 이미지 삭제",
            description = """
                업로드된 매거진 이미지를 삭제합니다.
                - 이미지를 실제로 사용하는 매거진의 작성자만 삭제할 수 있습니다.
                - 사용 중이지 않은 이미지는 1일이 지나면 자동으로 정리됩니다.
            """
    )
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long imageId) {
        magazineImageService.deleteImageById(principal.getUserId(), imageId);
        return ResponseEntity.noContent().build();
    }
}
