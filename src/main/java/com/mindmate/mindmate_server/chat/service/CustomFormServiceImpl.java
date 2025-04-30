package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.CustomForm;
import com.mindmate.mindmate_server.chat.domain.MessageType;
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
public class CustomFormServiceImpl implements CustomFormService {
    private final CustomFormRepository customFormRepository;

    private final UserService userService;
    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final ChatPresenceService chatPresenceService;


    // todo: chatservice에서 처리?
    @Override
    @Transactional
    public CustomFormResponse createCustomForm(Long userId, CustomFormRequest request) {
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getChatRoomId());

        chatRoomService.validateChatActivity(userId, request.getChatRoomId());

        CustomForm customForm = CustomForm.builder()
                .chatRoom(chatRoom)
                .creator(user)
                .responder(chatRoom.getListener().equals(user) ? chatRoom.getSpeaker() : chatRoom.getListener())
                .build();

        request.getQuestions().forEach(customForm::addItem);
        CustomForm savedForm = customFormRepository.save(customForm);

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user)
                .content("커스텀 폼이 생성되었습니다.")
                .type(MessageType.CUSTOM_FORM)
                .build();

        chatMessage.setCustomForm(savedForm);
        ChatMessage savedMessage = chatMessageService.save(chatMessage);
        chatRoom.updateLastMessageTime();

        User recipient = chatRoom.isListener(user) ? chatRoom.getSpeaker() : chatRoom.getListener();
        boolean isRecipientActive = chatPresenceService.isUserActiveInRoom(recipient.getId(), chatRoom.getId());

        chatService.publishMessageEvent(savedMessage, recipient.getId(), isRecipientActive, "커스텀 폼이 생성되었습니다.");

        return CustomFormResponse.from(savedForm);
    }

    @Override
    public CustomForm findCustomFormById(Long formId) {
        return customFormRepository.findById(formId)
                .orElseThrow(() -> new CustomException(CustomFormErrorCode.CUSTOM_FORM_NOT_FOUND));
    }

    // todo: 빈 답변 어떻게 처리할 것인가?
    @Override
    @Transactional
    public CustomFormResponse respondToCustomForm(Long formId, Long userId, RespondToCustomFormRequest request) {
        CustomForm customForm = findCustomFormById(formId);
        User user = userService.findUserById(userId);

        chatRoomService.validateChatActivity(userId, request.getChatRoomId());

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

        ChatMessage responseMessage = ChatMessage.builder()
                .chatRoom(customForm.getChatRoom())
                .sender(user)
                .content("커스텀 폼에 응답했습니다.")
                .type(MessageType.CUSTOM_FORM)
                .build();

        responseMessage.setCustomForm(updatedForm);
        ChatMessage savedMessage = chatMessageService.save(responseMessage);
        customForm.getChatRoom().updateLastMessageTime();

        User recipient = customForm.getChatRoom().isListener(user) ?
                customForm.getChatRoom().getSpeaker() : customForm.getChatRoom().getListener();
        boolean isRecipientActive = chatPresenceService.isUserActiveInRoom(
                recipient.getId(), customForm.getChatRoom().getId());

        chatService.publishMessageEvent(savedMessage, recipient.getId(), isRecipientActive, "커스텀 폼에 응답했습니다.");

        return CustomFormResponse.from(updatedForm);
    }

    @Override
    public List<CustomFormResponse> getCustomFormsByChatRoom(Long chatRoomId) {
        List<CustomForm> customForms = customFormRepository.findByChatRoomId(chatRoomId);
        return customForms.stream().map(CustomFormResponse::from).collect(Collectors.toList());
    }

}
