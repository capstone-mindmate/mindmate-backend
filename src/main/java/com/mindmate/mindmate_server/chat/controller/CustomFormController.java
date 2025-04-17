package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.CustomFormRequest;
import com.mindmate.mindmate_server.chat.dto.CustomFormResponse;
import com.mindmate.mindmate_server.chat.dto.RespondToCustomFormRequest;
import com.mindmate.mindmate_server.chat.service.CustomFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/custom-forms")
public class CustomFormController {
    private final CustomFormService customFormService;

    /**
     * 커스텀폼 생성
     */
    @PostMapping
    public ResponseEntity<CustomFormResponse> createCustomForm(@RequestBody CustomFormRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        CustomFormResponse response = customFormService.createCustomForm(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 커스텀폼 조회
     */
    @GetMapping("/{formId}")
    public ResponseEntity<CustomFormResponse> getCustomForm(@PathVariable Long formId) {
        return ResponseEntity.ok(CustomFormResponse.from(customFormService.findCustomFormById(formId)));
    }

    /**
     * 커스텀폼 답변하기
     * todo: 지금 질문과 답변의 순서가 일치한다는 보장으로 진행하고 있음 -> id 매핑을 해줘야 확실히 구분 가능
     */
    @PostMapping("/{formId}/respond")
    public ResponseEntity<CustomFormResponse> respondToCustomForm(@PathVariable Long formId,
                                                                  @RequestBody RespondToCustomFormRequest request,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        CustomFormResponse response = customFormService.respondToCustomForm(formId, principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }


    /**
     * 해당 채팅방의 모든 커스텀폼 조회
     */
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
