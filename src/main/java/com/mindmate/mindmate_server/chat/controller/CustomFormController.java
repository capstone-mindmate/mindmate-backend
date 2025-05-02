package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.CustomFormRequest;
import com.mindmate.mindmate_server.chat.dto.CustomFormResponse;
import com.mindmate.mindmate_server.chat.dto.RespondToCustomFormRequest;
import com.mindmate.mindmate_server.chat.service.CustomFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "커스텀 폼",
        description = "채팅방 내에서 사용하는 커스텀 폼(설문/질문지) 생성, 조회, 응답 관련 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/custom-forms")
public class CustomFormController {
    private final CustomFormService customFormService;

    @Operation(
            summary = "커스텀 폼 생성",
            description = "채팅방 내에서 커스텀 폼(설문/질문지)을 생성합니다. - websocket 장애시 사용"
    )
    @PostMapping
    public ResponseEntity<CustomFormResponse> createCustomForm(@RequestBody CustomFormRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        CustomFormResponse response = customFormService.createCustomForm(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "커스텀 폼 단건 조회",
            description = "특정 커스텀 폼의 상세 정보를 조회합니다."
    )
    @GetMapping("/{formId}")
    public ResponseEntity<CustomFormResponse> getCustomForm(@PathVariable Long formId) {
        return ResponseEntity.ok(CustomFormResponse.from(customFormService.findCustomFormById(formId)));
    }

    @Operation(
            summary = "커스텀 폼 응답 제출",
            description = "지정된 커스텀 폼에 대한 답변을 제출합니다. - websocket 장애시 사용"
    )
    @PostMapping("/{formId}/respond")
    public ResponseEntity<CustomFormResponse> respondToCustomForm(@PathVariable Long formId,
                                                                  @RequestBody RespondToCustomFormRequest request,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        CustomFormResponse response = customFormService.respondToCustomForm(formId, principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "채팅방 내 커스텀 폼 전체 조회",
            description = "특정 채팅방에서 생성된 모든 커스텀 폼 목록을 조회합니다."
    )
    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<List<CustomFormResponse>> getCustomFormsByChatRoom(@PathVariable Long chatRoomId) {
        List<CustomFormResponse> responses = customFormService.getCustomFormsByChatRoom(chatRoomId);
        return ResponseEntity.ok(responses);
    }
//
//    /**
//     * 사용자 생성 커스텀폼
//     */
//    @GetMapping("/user/created")
//    public ResponseEntity<List<CustomFormResponse>> getCustomFormsCreatedByUser(@AuthenticationPrincipal UserPrincipal principal) {
//        List<CustomFormResponse> responses = customFormService.getCustomFormsCreatedByUser(principal.getUserId());
//        return ResponseEntity.ok(responses);
//    }
//
//    /**
//     * 사용자 답변 커스텀폼
//     */
//    @GetMapping("/user/responded")
//    public ResponseEntity<List<CustomFormResponse>> getCustomFormsRespondedByUser(@AuthenticationPrincipal UserPrincipal principal) {
//        List<CustomFormResponse> responses = customFormService.getCustomFormsRespondedByUser(principal.getUserId());
//        return ResponseEntity.ok(responses);
//    }
}
