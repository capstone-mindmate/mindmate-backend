package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.CustomForm;
import com.mindmate.mindmate_server.chat.dto.CustomFormRequest;
import com.mindmate.mindmate_server.chat.dto.CustomFormResponse;
import com.mindmate.mindmate_server.chat.dto.RespondToCustomFormRequest;
import com.mindmate.mindmate_server.chat.repository.CustomFormRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.CustomFormErrorCode;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomFormService {
    private final CustomFormRepository customFormRepository;
    private final UserService userService;
    private final ChatRoomService chatRoomService;


    @Transactional
    public CustomFormResponse createCustomForm(Long userId, CustomFormRequest request) {
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getChatRoomId());

        chatRoomService.validateChatActivity(user, chatRoom);

        CustomForm customForm = CustomForm.builder()
                .chatRoom(chatRoom)
                .creator(user)
                .responder(chatRoom.getListener().equals(user) ? chatRoom.getSpeaker() : chatRoom.getListener())
                .build();

        request.getQuestions().forEach(customForm::addItem);

        CustomForm savedForm = customFormRepository.save(customForm);
        return CustomFormResponse.from(savedForm);
    }

    public CustomForm findCustomFormById(Long formId) {
        return customFormRepository.findById(formId)
                .orElseThrow(() -> new CustomException(CustomFormErrorCode.CUSTOM_FORM_NOT_FOUND));
    }

    @Transactional
    public CustomFormResponse respondToCustomForm(Long formId, Long userId, RespondToCustomFormRequest request) {
        CustomForm customForm = findCustomFormById(formId);
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getChatRoomId());

        chatRoomService.validateChatActivity(user, chatRoom);

        if (customForm.isAnswered()) {
            throw new CustomException(CustomFormErrorCode.CUSTOM_FORM_ALREADY_ANSWERED);
        }

        if (!customForm.getResponder().equals(user)) {
            throw new CustomException(CustomFormErrorCode.CUSTOM_FORM_INVALID_RESPONDER);
        }

        customForm.markAsAnswered();
        for (int i = 0; i < customForm.getItems().size(); i++) {
            customForm.getItems().get(i).setAnswer(request.getAnswers().get(i));;
        }

        CustomForm updatedForm = customFormRepository.save(customForm);
        return CustomFormResponse.from(updatedForm);
    }

    public List<CustomFormResponse> getCustomFormsByChatRoom(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(chatRoomId);

        List<CustomForm> customForms = customFormRepository.findByChatRoomId(chatRoomId);
        return customForms.stream().map(CustomFormResponse::from).collect(Collectors.toList());
    }

}
