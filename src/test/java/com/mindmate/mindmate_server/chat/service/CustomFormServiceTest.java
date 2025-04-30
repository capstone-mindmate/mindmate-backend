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
    @Mock private ChatPresenceService chatPresenceService;

    @InjectMocks
    private CustomFormServiceImpl customFormService;

    private static final Long CREATOR_ID = 1L;
    private static final Long RESPONDER_ID = 2L;
    private static final Long CHAT_ROOM_ID = 100L;
    private static final Long FORM_ID = 1L;

    private User mockCreator;
    private User mockResponder;
    private ChatRoom mockChatRoom;
    private CustomForm mockCustomForm;

    @BeforeEach
    void setup() {
        mockCreator = mock(User.class);
        mockResponder = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);
        mockCustomForm = mock(CustomForm.class);

        when(mockCreator.getId()).thenReturn(CREATOR_ID);
        when(mockResponder.getId()).thenReturn(RESPONDER_ID);
        when(mockChatRoom.getId()).thenReturn(CHAT_ROOM_ID);
        when(mockChatRoom.getListener()).thenReturn(mockCreator);
        when(mockChatRoom.getSpeaker()).thenReturn(mockResponder);

        when(mockChatRoom.isListener(mockCreator)).thenReturn(true);
        when(mockChatRoom.isSpeaker(mockCreator)).thenReturn(false);
        when(mockChatRoom.isListener(mockResponder)).thenReturn(false);
        when(mockChatRoom.isSpeaker(mockResponder)).thenReturn(true);

        when(userService.findUserById(CREATOR_ID)).thenReturn(mockCreator);
        when(userService.findUserById(RESPONDER_ID)).thenReturn(mockResponder);
        when(chatRoomService.findChatRoomById(CHAT_ROOM_ID)).thenReturn(mockChatRoom);

        when(mockCustomForm.getId()).thenReturn(FORM_ID);
        when(mockCustomForm.getChatRoom()).thenReturn(mockChatRoom);
        when(mockCustomForm.getCreator()).thenReturn(mockCreator);
        when(mockCustomForm.getResponder()).thenReturn(mockResponder);
        when(mockCustomForm.getItems()).thenReturn(new ArrayList<>());
    }

    private ChatMessage createCustomFormMessage(User sender, String content) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(mockChatRoom)
                .sender(sender)
                .content(content)
                .type(MessageType.CUSTOM_FORM)
                .build();
        message.setCustomForm(mockCustomForm);
        return message;
    }

    @Nested
    @DisplayName("커스텀 폼 생성 테스트")
    class CreateCustomFormTest {
        @Test
        @DisplayName("커스텀 폼 생성 성공")
        void createCustomForm_Success() {
            // given
            List<String> questions = Arrays.asList("질문1", "질문2", "질문3");
            CustomFormRequest request = new CustomFormRequest(CHAT_ROOM_ID, questions);
            when(customFormRepository.save(any(CustomForm.class))).thenReturn(mockCustomForm);

            ChatMessage message = createCustomFormMessage(mockCreator, "커스텀 폼이 생성되었습니다.");
            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(message);
            when(chatPresenceService.isUserActiveInRoom(RESPONDER_ID, CHAT_ROOM_ID)).thenReturn(true);

            // when
            CustomFormResponse responses = customFormService.createCustomForm(CREATOR_ID, request);

            // then
            assertNotNull(responses);
            verify(chatRoomService).validateChatActivity(CREATOR_ID, CHAT_ROOM_ID);
            verify(customFormRepository).save(any(CustomForm.class));
            verify(chatMessageService).save(any(ChatMessage.class));
            verify(chatService).publishMessageEvent(
                    eq(message),
                    eq(RESPONDER_ID),
                    eq(true),
                    eq("커스텀 폼이 생성되었습니다.")
            );
        }

        @Test
        @DisplayName("채팅방 활동 검증 실패")
        void createCustomForm_InvalidActivity() {
            // given
            List<String> questions = Arrays.asList("질문1", "질문2", "질문3");
            CustomFormRequest request = new CustomFormRequest(CHAT_ROOM_ID, questions);
            doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                    .when(chatRoomService).validateChatActivity(CREATOR_ID, CHAT_ROOM_ID);

            // when & then
            assertThrows(CustomException.class, () -> customFormService.createCustomForm(CREATOR_ID, request));
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
            RespondToCustomFormRequest request = new RespondToCustomFormRequest(FORM_ID, CHAT_ROOM_ID, answers);

            List<CustomFormItem> items = IntStream.range(0, 3)
                    .mapToObj(i -> mock(CustomFormItem.class))
                    .collect(Collectors.toList());

            when(customFormRepository.findById(FORM_ID)).thenReturn(Optional.of(mockCustomForm));
            when(mockCustomForm.isAnswered()).thenReturn(false);
            when(mockCustomForm.getItems()).thenReturn(items);
            when(customFormRepository.save(mockCustomForm)).thenReturn(mockCustomForm);

            ChatMessage message = createCustomFormMessage(mockResponder, "커스텀 폼에 응답했습니다.");
            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(message);
            when(chatPresenceService.isUserActiveInRoom(CREATOR_ID, CHAT_ROOM_ID)).thenReturn(false);

            // when
            CustomFormResponse response = customFormService.respondToCustomForm(FORM_ID, RESPONDER_ID, request);

            // then
            assertNotNull(response);
            verify(chatRoomService).validateChatActivity(RESPONDER_ID, CHAT_ROOM_ID);
            verify(mockCustomForm).markAsAnswered();
            verify(customFormRepository).save(mockCustomForm);
            verify(chatMessageService).save(any(ChatMessage.class));
            verify(chatService).publishMessageEvent(
                    eq(message),
                    eq(CREATOR_ID),
                    eq(false),
                    eq("커스텀 폼에 응답했습니다.")
            );
        }

        @Test
        @DisplayName("의미 응답한 폼에 응답 시도")
        void respondToCustomForm_AlreadyAnswered() {
            // given
            List<String> answers = Arrays.asList("답변1", "답변2", "답변3");
            RespondToCustomFormRequest request = new RespondToCustomFormRequest(FORM_ID, CHAT_ROOM_ID, answers);

            when(customFormRepository.findById(FORM_ID)).thenReturn(Optional.of(mockCustomForm));
            when(mockCustomForm.isAnswered()).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> customFormService.respondToCustomForm(FORM_ID, RESPONDER_ID, request));
            verify(mockCustomForm, never()).markAsAnswered();
            verify(customFormRepository, never()).save(any(CustomForm.class));
        }

        @Test
        @DisplayName("응답자가 아닌 사용자가 응답 시도")
        void respondToCustomForm_InvalidResponder() {
            // given
            List<String> answers = Arrays.asList("답변1", "답변2", "답변3");
            RespondToCustomFormRequest request = new RespondToCustomFormRequest(FORM_ID, CHAT_ROOM_ID, answers);

            when(customFormRepository.findById(FORM_ID)).thenReturn(Optional.of(mockCustomForm));
            when(mockCustomForm.isAnswered()).thenReturn(false);
            when(mockCustomForm.getResponder()).thenReturn(mockResponder);

            // when & then
            assertThrows(CustomException.class, () -> customFormService.respondToCustomForm(FORM_ID, CREATOR_ID, request));
            verify(mockCustomForm, never()).markAsAnswered();
            verify(customFormRepository, never()).save(any(CustomForm.class));
        }
    }

    @Test
    @DisplayName("ID로 커스텀 폼 조회")
    void findCustomFormById_Success() {
        // given
        when(customFormRepository.findById(FORM_ID)).thenReturn(Optional.of(mockCustomForm));

        // when
        CustomForm result = customFormService.findCustomFormById(FORM_ID);

        // then
        assertNotNull(result);
        assertEquals(FORM_ID, result.getId());
    }

    @Test
    @DisplayName("존재하지 않는 커스텀 폼 조회")
    void findCustomFormById_NotFound() {
        // given
        when(customFormRepository.findById(FORM_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CustomException.class, () -> customFormService.findCustomFormById(FORM_ID));
    }

    @Test
    @DisplayName("채팅방별 커스텀 폼 목록 조회")
    void getCustomFormByChatRoom_Success() {
        // given
        List<CustomForm> forms = Arrays.asList(mockCustomForm);
        when(customFormRepository.findByChatRoomId(CHAT_ROOM_ID)).thenReturn(forms);

        // when
        List<CustomFormResponse> result = customFormService.getCustomFormsByChatRoom(CHAT_ROOM_ID);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}