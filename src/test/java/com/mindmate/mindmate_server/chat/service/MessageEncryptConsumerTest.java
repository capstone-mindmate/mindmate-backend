//package com.mindmate.mindmate_server.chat.service;
//
//import com.mindmate.mindmate_server.chat.domain.ChatMessage;
//import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
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
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class MessageEncryptConsumerTest {
//    @Mock private AesGcmEncryptionService aesGcmEncryptionService;
//    @Mock private ChatMessageService chatMessageService;
//    @Mock private ConsumerRecord<String , ChatMessageEvent> mockRecord;
//    @Mock private ChatMessageEvent mockEvent;
//    @Mock private ChatMessage mockMessage;
//
//    @InjectMocks
//    private MessageEncryptConsumer messageEncryptConsumer;
//
//    private final Long messageId = 1L;
//    private final String originalContent = "원본 메시지 내용";
//    private final String encryptedContent = "암호화된 내용";
//
//    @BeforeEach
//    void setup() {
//        when(mockRecord.value()).thenReturn(mockEvent);
//        when(mockEvent.getMessageId()).thenReturn(messageId);
//        when(chatMessageService.findChatMessageById(messageId)).thenReturn(mockMessage);
//        when(mockMessage.getContent()).thenReturn(originalContent);
//        when(aesGcmEncryptionService.encrypt(originalContent)).thenReturn(encryptedContent);
//    }
//
//    @ParameterizedTest
//    @DisplayName("처리하지 않아야 하는 메시지 시나리오")
//    @MethodSource("noProcessingScenarios")
//    void encryptMessage_ShouldNotProcess(
//            String testCase,
//            boolean isFiltered,
//            boolean isEncrypted,
//            Long messageId
//    ) {
//        // given
//        when(mockEvent.isFiltered()).thenReturn(isFiltered);
//        when(mockEvent.isEncrypted()).thenReturn(isEncrypted);
//        when(mockEvent.getMessageId()).thenReturn(messageId);
//
//        // when
//        messageEncryptConsumer.encryptMessage(mockRecord);
//
//        // then
//        verify(chatMessageService, never()).findChatMessageById(anyLong());
//        verify(aesGcmEncryptionService, never()).encrypt(anyString());
//        verify(chatMessageService, never()).save(any(ChatMessage.class));
//    }
//
//    static Stream<Arguments> noProcessingScenarios() {
//        return Stream.of(
//                Arguments.of("필터링된 메시지", true, false, 1L),
//                Arguments.of("이미 암호화된 메시지", false, true, 1L),
//                Arguments.of("메시지 ID가 null인 경우", false, false, null)
//        );
//    }
//
//    @Test
//    @DisplayName("메시지 암호화 성공")
//    void encryptMessage_Success() {
//        // given
//        when(mockEvent.isFiltered()).thenReturn(false);
//        when(mockEvent.isEncrypted()).thenReturn(false);
//
//        // when
//        messageEncryptConsumer.encryptMessage(mockRecord);
//
//        // then
//        verify(chatMessageService).findChatMessageById(messageId);
//        verify(aesGcmEncryptionService).encrypt(originalContent);
//        verify(mockMessage).updateEncryptedContent(encryptedContent);
//        verify(chatMessageService).save(mockMessage);
//    }
//
//}