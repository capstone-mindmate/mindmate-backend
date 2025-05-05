package com.mindmate.mindmate_server.payment.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.payment.dto.*;
import com.mindmate.mindmate_server.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
@Tag(name = "결제", description = "코인 구매를 위한 결제 API")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "상품 목록 조회", description = "구매 가능한 코인 상품 목록을 조회합니다.")
    @GetMapping("/products")
    public ResponseEntity<List<PaymentProductResponse>> getProducts() {
        return ResponseEntity.ok(paymentService.getProducts());
    }

    @Operation(summary = "결제 주문 생성", description = "코인 상품 구매를 위한 결제 주문을 생성합니다.")
    @PostMapping("/orders")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateOrderRequest request) {
        PaymentOrderResponse response = paymentService.createOrder(principal.getUserId(), request.getProductId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 성공 처리", description = "결제 성공 후 코인을 적립합니다.")
    @GetMapping("/success")
    public ResponseEntity<PaymentConfirmResponse> paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Integer price) {

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .price(price)
                .build();

        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 실패 처리", description = "결제 실패시 오류 정보를 제공합니다.")
    @GetMapping("/fail")
    public ResponseEntity<PaymentFailResponse> paymentFail(
            @RequestParam String orderId,
            @RequestParam String errorCode,
            @RequestParam String errorMsg) {

        PaymentFailResponse response = PaymentFailResponse.builder()
                .orderId(orderId)
                .errorCode(errorCode)
                .errorMessage(errorMsg)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 설정 정보", description = "프론트엔드에서 필요한 결제 설정 정보를 제공합니다.")
    @GetMapping("/config")
    public ResponseEntity<PaymentConfigResponse> getPaymentConfig() {
        return ResponseEntity.ok(paymentService.getPaymentConfig());
    }

    @Operation(summary = "결제 내역 조회", description = "사용자의 결제 내역을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PaymentHistoryResponse> history = paymentService.getUserPaymentHistory(principal.getUserId());
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "결제 상세 조회", description = "특정 결제의 상세 정보를 조회합니다.")
    @GetMapping("/details/{orderId}")
    public ResponseEntity<PaymentDetailResponse> getPaymentDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId) {
        PaymentDetailResponse detail = paymentService.getPaymentDetail(principal.getUserId(), orderId);
        return ResponseEntity.ok(detail);
    }
}