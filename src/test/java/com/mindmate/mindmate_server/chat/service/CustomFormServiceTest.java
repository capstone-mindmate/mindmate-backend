package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.*;
import com.mindmate.mindmate_server.chat.dto.CustomFormRequest;
import com.mindmate.mindmate_server.chat.dto.CustomFormResponse;
import com.mindmate.mindmate_server.chat.dto.RespondToCustomFormRequest;
import com.mindmate.mindmate_server.chat.repository.CustomFormRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomFormServiceTest {
    @Mock private CustomFormRepository customFormRepository;
    @Mock private UserService userService;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ChatService chatService;
    @Mock private ChatMessageService chatMessageService;

    @InjectMocks
    private CustomFormService customFormService;

    private Long userId;
    private Long chatRoomId;
    private Long formId;
    private User mockCreator;
    private User mockResponder;
    private ChatRoom mockChatRoom;
    private CustomForm mockCustomForm;

    @BeforeEach
    void setup() {
        userId = 1L;
        chatRoomId = 100L;
        formId = 1L;

        mockCreator = mock(User.class);
        mockResponder = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);
        mockCustomForm = mock(CustomForm.class);

        when(mockCreator.getId()).thenReturn(userId);
        when(mockResponder.getId()).thenReturn(2L);
        when(mockChatRoom.getId()).thenReturn(chatRoomId);
        when(mockChatRoom.getListener()).thenReturn(mockCreator);
        when(mockChatRoom.getSpeaker()).thenReturn(mockResponder);

        when(userService.findUserById(userId)).thenReturn(mockCreator);
        when(userService.findUserById(2L)).thenReturn(mockResponder);
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(mockChatRoom);

        when(mockCustomForm.getId()).thenReturn(formId);
        when(mockCustomForm.getChatRoom()).thenReturn(mockChatRoom);
        when(mockCustomForm.getCreator()).thenReturn(mockCreator);
        when(mockCustomForm.getResponder()).thenReturn(mockResponder);
        when(mockCustomForm.getItems()).thenReturn(new ArrayList<>());
    }

    @Nested
    @DisplayName("커스텀 폼 생성 테스트")
    class CreateCustomFormTest {
        @Test
        @DisplayName("커스텀 폼 생성 성공")
        void createCustomForm_Success() {
            // given
            List<String> questions = Arrays.asList("질문1", "질문2", "질문3");
            CustomFormRequest request = new CustomFormRequest(chatRoomId, questions);
            CustomForm form = new CustomForm(mockChatRoom, mockCreator, mockResponder);
            when(customFormRepository.save(any(CustomForm.class))).thenReturn(form);

            ChatMessage message = ChatMessage.builder()
                    .chatRoom(mockChatRoom)
                    .sender(mockCreator)
                    .content("커스텀 폼이 생성되었습니다.")
                    .type(MessageType.CUSTOM_FORM)
                    .build();

            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(message);

            // when
            CustomFormResponse responses = customFormService.createCustomForm(userId, request);

            // then
            assertNotNull(responses);
            verify(chatRoomService).validateChatActivity(userId, chatRoomId);
            verify(customFormRepository).save(any(CustomForm.class));
            verify(chatMessageService).save(any(ChatMessage.class));
            verify(chatService).publishMessageEvent(any(ChatMessage.class));
        }

        @Test
        @DisplayName("채팅방 활동 검증 실패")
        void createCustomForm_InvalidActivity() {
            // given
            List<String> questions = Arrays.asList("질문1", "질문2", "질문3");
            CustomFormRequest request = new CustomFormRequest(chatRoomId, questions);
            doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                    .when(chatRoomService).validateChatActivity(userId, chatRoomId);

            // when & then
            assertThrows(CustomException.class, () -> customFormService.createCustomForm(userId, request));
            verify(customFormRepository, never()).save(any(CustomForm.class));
        }
    }

    @Nested
    @DisplayName("커스텀 폼 응답 테스트")
    class RespondCustomFormTest {
        @Test
        @DisplayName("커스텀 폼 응답 성공")
        void respondToCustomForm_Success() {
            // given
            List<String> answers = Arrays.asList("답변1", "답변2", "답변3");
            RespondToCustomFormRequest request = new RespondToCustomFormRequest(formId, chatRoomId, answers);

            List<CustomFormItem> items = IntStream.range(0, 3)
                    .mapToObj(i -> {
                        CustomFormItem item = mock(CustomFormItem.class);
                        return item;
                    })
                    .collect(Collectors.toList());

            when(customFormRepository.findById(formId)).thenReturn(Optional.of(mockCustomForm));
            when(mockCustomForm.isAnswered()).thenReturn(false);
            when(mockCustomForm.getItems()).thenReturn(items);
            when(customFormRepository.save(mockCustomForm)).thenReturn(mockCustomForm);

            ChatMessage message = ChatMessage.builder()
                    .chatRoom(mockChatRoom)
                    .sender(mockResponder)
                    .content("커스텀 폼에 응답했습니다.")
                    .type(MessageType.CUSTOM_FORM)
                    .build();

            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(message);

            // when
            CustomFormResponse response = customFormService.respondToCustomForm(formId, 2L, request);

            // then
            assertNotNull(response);
            verify(chatRoomService).validateChatActivity(2L, chatRoomId);
            verify(mockCustomForm).markAsAnswered();
            verify(customFormRepository).save(mockCustomForm);
            verify(chatMessageService).save(any(ChatMessage.class));
            verify(chatService).publishMessageEvent(any(ChatMessage.class));
        }

        @Test
        @DisplayName("의미 응답한 폼에 응답 시도")
        void respondToCustomForm_AlreadyAnswered() {
            // given
            List<String> answers = Arrays.asList("답변1", "답변2", "답변3");
            RespondToCustomFormRequest request = new RespondToCustomFormRequest(formId, chatRoomId, answers);

            when(customFormRepository.findById(formId)).thenReturn(Optional.of(mockCustomForm));
            when(mockCustomForm.isAnswered()).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> customFormService.respondToCustomForm(formId, 2L, request));
            verify(mockCustomForm, never()).markAsAnswered();
            verify(customFormRepository, never()).save(any(CustomForm.class));
        }

        @Test
        @DisplayName("응답자가 아닌 사용자가 응답 시도")
        void respondToCustomForm_InvalidResponder() {
            // given
            List<String> answers = Arrays.asList("답변1", "답변2", "답변3");
            RespondToCustomFormRequest request = new RespondToCustomFormRequest(formId, chatRoomId, answers);

            when(customFormRepository.findById(formId)).thenReturn(Optional.of(mockCustomForm));
            when(mockCustomForm.isAnswered()).thenReturn(false);
            when(mockCustomForm.getResponder()).thenReturn(mockResponder);

            // when & then
            assertThrows(CustomException.class, () -> customFormService.respondToCustomForm(formId, userId, request));
            verify(mockCustomForm, never()).markAsAnswered();
            verify(customFormRepository, never()).save(any(CustomForm.class));
        }
    }
    @Test
    @DisplayName("ID로 커스텀 폼 조회")
    void findCustomFormById_Success() {
        // given
        when(customFormRepository.findById(formId)).thenReturn(Optional.of(mockCustomForm));

        // when
        CustomForm result = customFormService.findCustomFormById(formId);

        // then
        assertNotNull(result);
        assertEquals(formId, result.getId());
    }

    @Test
    @DisplayName("존재하지 않는 커스텀 폼 조회")
    void findCustomFormById_NotFound() {
        // given
        when(customFormRepository.findById(formId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CustomException.class, () -> customFormService.findCustomFormById(formId));
    }

    @Test
    @DisplayName("채팅방별 커스텀 폼 목록 조회")
    void getCustomFormByChatRoom_Success() {
        // given
        List<CustomForm> forms = Arrays.asList(mockCustomForm);
        when(customFormRepository.findByChatRoomId(chatRoomId)).thenReturn(forms);

        // when
        List<CustomFormResponse> result = customFormService.getCustomFormsByChatRoom(chatRoomId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}