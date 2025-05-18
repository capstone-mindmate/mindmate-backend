package com.mindmate.mindmate_server.payment.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.payment.dto.PaymentProductRequest;
import com.mindmate.mindmate_server.payment.dto.PaymentProductResponse;
import com.mindmate.mindmate_server.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/products")
@Tag(name = "결제 상품", description = "포인트 결제 상품 관리 API")
public class AdminProductController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentProductResponse>> getAllProducts(
            @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(paymentService.getAllProducts(active));
    }

    @Operation(summary = "포인트 상품 등록", description = "새로운 포인트 상품을 등록합니다. 관리자 권한이 필요합니다.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentProductResponse> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PaymentProductRequest request) {
        return ResponseEntity.ok(paymentService.createProduct(request));
    }

    @Operation(summary = "포인트 상품 수정", description = "기존 포인트 상품 정보를 수정합니다. 관리자 권한이 필요합니다.")
    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentProductResponse> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @RequestBody PaymentProductRequest request) {
        return ResponseEntity.ok(paymentService.updateProduct(productId, request));
    }

    @Operation(summary = "포인트 상품 활성화/비활성화", description = "포인트 상품의 활성화 상태를 변경합니다. 관리자 권한이 필요합니다.")
    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentProductResponse> toggleProductStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        return ResponseEntity.ok(paymentService.toggleProductStatus(productId));
    }

    @Operation(summary = "포인트 상품 삭제", description = "포인트 상품을 삭제합니다. 관리자 권한이 필요합니다.")
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        paymentService.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }
}