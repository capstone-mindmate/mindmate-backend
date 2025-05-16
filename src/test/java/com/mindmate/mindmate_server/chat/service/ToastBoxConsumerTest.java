//package com.mindmate.mindmate_server.chat.service;
//
//import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
//import com.mindmate.mindmate_server.chat.dto.ChatEventType;
//import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
//import com.mindmate.mindmate_server.chat.dto.ToastBoxEvent;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.DisplayNameGeneration;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class ToastBoxConsumerTest {
//    @Mock private ToastBoxService toastBoxService;
//    @Mock private ChatEventPublisher chatEventPublisher;
//    @Mock private ConsumerRecord<String, ChatMessageEvent> mockRecord;
//    @Mock private ChatMessageEvent mockEvent;
//
//    @InjectMocks
//    private ToastBoxConsumer toastBoxConsumer;
//
//    private final Long roomId = 100L;
//    private final Long messageId = 1000L;
//    private final String content = "테스트 내용 키워드 포함";
//
//    @BeforeEach
//    void setup() {
//        when(mockRecord.value()).thenReturn(mockEvent);
//        when(mockEvent.getRoomId()).thenReturn(roomId);
//        when(mockEvent.getMessageId()).thenReturn(messageId);
//        when(mockEvent.getContent()).thenReturn(content);
//        when(mockEvent.isFiltered()).thenReturn(false);
//    }
//
//
//    @ParameterizedTest
//    @DisplayName("처리하지 않아야 하는 메시지 시나리오")
//    @MethodSource("noProcessingScenarios")
//    void processToastBox_ShouldNotProcess(
//            String testCase,
//            boolean isFiltered,
//            Long messageId
//    ) {
//        // given
//        when(mockEvent.isFiltered()).thenReturn(isFiltered);
//        when(mockEvent.getMessageId()).thenReturn(messageId);
//
//        // when
//        toastBoxConsumer.processToastBox(mockRecord);
//
//        // then
//        verify(toastBoxService, never()).findToastBoxKeywords(anyString());
//        verify(chatEventPublisher, never()).publishChatRoomEvent(anyLong(), any(), any());
//    }
//
//    static Stream<Arguments> noProcessingScenarios() {
//        return Stream.of(
//                Arguments.of("필터링된 메시지", true, 1L),
//                Arguments.of("메시지 ID가 null인 경우", false, null)
//        );
//    }
//
//    @Test
//    @DisplayName("키워드가 없는 메시지 처리")
//    void processToastBox_NoKeywords() {
//        // given
//        when(toastBoxService.findToastBoxKeywords(content)).thenReturn(Collections.emptyList());
//
//        // when
//        toastBoxConsumer.processToastBox(mockRecord);
//
//        // then
//        verify(toastBoxService).findToastBoxKeywords(content);
//        verify(chatEventPublisher, never()).publishChatRoomEvent(anyLong(), any(), any());
//    }
//
//    @Test
//    @DisplayName("키우드가 있는 메시지 처리 - 단일 키워드")
//    void processToastBox_SingleKeyword() {
//        // given
//        ToastBoxKeyword keyword = createKeyword("키워드", "제목", "내용");
//        when(toastBoxService.findToastBoxKeywords(content)).thenReturn(List.of(keyword));
//
//        // when
//        toastBoxConsumer.processToastBox(mockRecord);
//
//        // then
//        verify(toastBoxService).findToastBoxKeywords(content);
//        verify(chatEventPublisher).publishChatRoomEvent(
//                eq(roomId),
//                eq(ChatEventType.TOAST_BOX),
//                argThat(event -> {
//                    ToastBoxEvent toastEvent = (ToastBoxEvent) event;
//                    return toastEvent.getRoomId().equals(roomId) &&
//                            toastEvent.getMessageId().equals(messageId) &&
//                            toastEvent.getKeyword().equals("키워드") &&
//                            toastEvent.getTitle().equals("제목") &&
//                            toastEvent.getContent().equals("내용");
//                })
//        );
//    }
//
//    @Test
//    @DisplayName("키워드가 있는 메시지 처리 - 다중 키워드")
//    void processToastBox_MultipleKeywords() {
//        // given
//        List<ToastBoxKeyword> keywords = List.of(
//                createKeyword("키워드1", "제목1", "내용1"),
//                createKeyword("키워드2", "제목2", "내용2")
//        );
//        when(toastBoxService.findToastBoxKeywords(content)).thenReturn(keywords);
//
//        // when
//        toastBoxConsumer.processToastBox(mockRecord);
//
//        // then
//        verify(toastBoxService).findToastBoxKeywords(content);
//        verify(chatEventPublisher, times(2)).publishChatRoomEvent(
//                eq(roomId),
//                eq(ChatEventType.TOAST_BOX),
//                any(ToastBoxEvent.class)
//        );
//    }
//
//    private ToastBoxKeyword createKeyword(String keyword, String title, String content) {
//        ToastBoxKeyword mock = mock(ToastBoxKeyword.class);
//        when(mock.getKeyword()).thenReturn(keyword);
//        when(mock.getTitle()).thenReturn(title);
//        when(mock.getContent()).thenReturn(content);
//        when(mock.getLinkUrl()).thenReturn("http://example.com");
//        when(mock.getImageUrl()).thenReturn("http://example.com/image.jpg");
//        return mock;
//    }
//
//
//}