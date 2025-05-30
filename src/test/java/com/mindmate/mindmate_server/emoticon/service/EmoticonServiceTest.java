package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.service.ChatMessageService;
import com.mindmate.mindmate_server.chat.service.ChatPresenceService;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.chat.service.ChatService;
import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonType;
import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import com.mindmate.mindmate_server.emoticon.dto.*;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.emoticon.repository.UserEmoticonRepository;
import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.EmoticonErrorCode;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmoticonServiceTest {
    @Mock private EmoticonRepository emoticonRepository;
    @Mock private UserEmoticonRepository userEmoticonRepository;
    @Mock private UserService userService;
    @Mock private PointService pointService;
    @Mock private SlackNotifier slackNotifier;
    @Mock private FileStorageService fileStorageService;
    @Mock private EmoticonInteractionService emoticonInteractionService;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ChatMessageService chatMessageService;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private ChatService chatService;

    @InjectMocks
    private EmoticonServiceImpl emoticonService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long EMOTICON_ID = 100L;
    private static final Long ROOM_ID = 200L;
    private static final String EMOTICON_NAME = "Test Emoticon";
    private static final String EMOTICON_URL = "test/emoticons/uuid.png";
    private static final int EMOTICON_PRICE = 1000;
    private static final String EMOTICON_DIR = "./uploads/emoticons/";
    private static final String EMOTICON_URL_PREFIX = "/emoticons/";

    private User mockUser;
    private User mockOtherUser;
    private Emoticon mockEmoticon;
    private UserEmoticon mockUserEmoticon;
    private MultipartFile mockFile;
    private Profile mockProfile;

    @BeforeEach
    void setup() {
        setupMockObjects();
        setupRepositoryMocks();
        setupServiceMocks();
        setupFieldInjection();
    }

    private void setupMockObjects() {
        mockUser = createMockUser(USER_ID, "testUser");
        mockOtherUser = createMockUser(OTHER_USER_ID, "otherUser");
        mockEmoticon = createMockEmoticon();
        mockUserEmoticon = createMockUserEmoticon();
        mockFile = mock(MultipartFile.class);
    }

    private User createMockUser(Long id, String nickname) {
        User user = mock(User.class);
        Profile profile = mock(Profile.class);

        when(user.getId()).thenReturn(id);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getNickname()).thenReturn(nickname);

        return user;
    }

    private Emoticon createMockEmoticon() {
        Emoticon emoticon = mock(Emoticon.class);
        when(emoticon.getId()).thenReturn(EMOTICON_ID);
        when(emoticon.getName()).thenReturn(EMOTICON_NAME);
        when(emoticon.getImageUrl()).thenReturn(EMOTICON_URL);
        when(emoticon.getPrice()).thenReturn(EMOTICON_PRICE);
        when(emoticon.isDefault()).thenReturn(false);
        when(emoticon.getStatus()).thenReturn(EmoticonStatus.ACCEPT);
        when(emoticon.getCreator()).thenReturn(mockUser);
        return emoticon;
    }

    private UserEmoticon createMockUserEmoticon() {
        UserEmoticon userEmoticon = mock(UserEmoticon.class);
        when(userEmoticon.getUser()).thenReturn(mockUser);
        when(userEmoticon.getEmoticon()).thenReturn(mockEmoticon);
        when(userEmoticon.getType()).thenReturn(EmoticonType.PURCHASED);
        return userEmoticon;
    }

    private void setupRepositoryMocks() {
        when(emoticonRepository.findById(EMOTICON_ID)).thenReturn(Optional.of(mockEmoticon));
        when(emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT))
                .thenReturn(List.of(mockEmoticon));
        when(emoticonRepository.findSimilarPriceEmoticons(
                any(EmoticonStatus.class), anyBoolean(), anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(Collections.emptyList());
    }

    private void setupServiceMocks() {
        when(userService.findUserById(USER_ID)).thenReturn(mockUser);
        when(userService.findUserById(OTHER_USER_ID)).thenReturn(mockOtherUser);

        doNothing().when(emoticonInteractionService).handlePurchase(anyLong());
        doNothing().when(emoticonInteractionService).incrementViewCount(anyLong(), anyLong());
        doNothing().when(emoticonInteractionService).incrementUsage(anyLong());
    }

    private void setupFieldInjection() {
        ReflectionTestUtils.setField(emoticonService, "emoticonDir", EMOTICON_DIR);
        ReflectionTestUtils.setField(emoticonService, "emoticonUrlPrefix", EMOTICON_URL_PREFIX);
    }

    @Nested
    @DisplayName("이모티콘 샾 조회 테스트")
    class GetShopEmoticonsTest {
        @ParameterizedTest
        @DisplayName("이모티콘 샾 조회 테스트")
        @MethodSource("shopEmoticonScenarios")
        void getShopEmoticons_UserScenarios(String description, Long userId, boolean expectedPurchased) {
            // given
            if (userId != null && userId.equals(USER_ID)) {
                when(userEmoticonRepository.findByUserId(userId)).thenReturn(List.of(mockUserEmoticon));
            } else if (userId != null) {
                when(userEmoticonRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            }

            // when
            List<EmoticonResponse> responses = emoticonService.getShopEmoticons(userId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(EMOTICON_ID);
            assertThat(responses.get(0).isPurchased()).isEqualTo(expectedPurchased);
        }

        static Stream<Arguments> shopEmoticonScenarios() {
            return Stream.of(
                    Arguments.of("구매한 사용자", USER_ID, true),
                    Arguments.of("구매하지 않은 사용자", OTHER_USER_ID, false),
                    Arguments.of("비로그인 사용자", null, false)
            );
        }
    }

    @Nested
    @DisplayName("이모티콘 상세 조회 테스트")
    class GetEmoticonDetailTest {
        @ParameterizedTest
        @DisplayName("이모티콘 상세 조회 성공")
        @MethodSource("detailScenarios")
        void getEmoticonDetail_UserScenarios(String description, Long userId, boolean hasPurchased) {
            // given
            if (userId != null) {
                when(userEmoticonRepository.existsByUserIdAndEmoticonId(userId, EMOTICON_ID)).thenReturn(hasPurchased);
                when(userEmoticonRepository.findByUserId(userId)).thenReturn(hasPurchased ? List.of(mockUserEmoticon) : Collections.emptyList());
            }

            // when
            EmoticonDetailResponse response = emoticonService.getEmoticonDetail(EMOTICON_ID, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getEmoticon().getId()).isEqualTo(EMOTICON_ID);
            assertThat(response.getEmoticon().isPurchased()).isEqualTo(hasPurchased);
            verify(emoticonInteractionService).incrementViewCount(EMOTICON_ID, userId);
        }

        static Stream<Arguments> detailScenarios() {
            return Stream.of(
                    Arguments.of("구매한 사용자", USER_ID, true),
                    Arguments.of("구매하지 않은 사용자", USER_ID, false),
                    Arguments.of("비로그인 사용자", null, false)
            );
        }

        @Test
        @DisplayName("이모티콘 상세 조회 실패 - 존재하지 않는 이모티콘")
        void getEmoticonDetail_NotFound() {
            // given
            when(emoticonRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> emoticonService.getEmoticonDetail(999L, USER_ID));
            assertEquals(EmoticonErrorCode.EMOTICON_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("이모티콘 구매 테스트")
    class PurchaseEmoticonTest {
        @ParameterizedTest
        @DisplayName("이모티콘 구매 시나리오")
        @CsvSource({
                "false, 2000, true", // 구매 안함, 충분한 포인트, 성공
                "true, 2000, false", // 이미 구매함, 충분한 포인트, 실패
                "false, 500, false", // 구매 안함, 부족한 포인트, 실패
        })
        void purchaseEmoticon_Scenarios(boolean alreadyPurchased, int userPoints, boolean shouldSucceed) {
            // given
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(USER_ID, EMOTICON_ID))
                    .thenReturn(alreadyPurchased);
            when(pointService.getCurrentBalance(USER_ID)).thenReturn(userPoints);
            when(userEmoticonRepository.save(any(UserEmoticon.class))).thenReturn(mockUserEmoticon);

            // when & then
            if (shouldSucceed) {
                EmoticonResponse response = emoticonService.purchaseEmoticon(USER_ID, EMOTICON_ID);

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(EMOTICON_ID);
                verify(userEmoticonRepository).save(any(UserEmoticon.class));
                verify(emoticonInteractionService).handlePurchase(EMOTICON_ID);
            } else {
                assertThrows(CustomException.class,
                        () -> emoticonService.purchaseEmoticon(USER_ID, EMOTICON_ID));
                verify(emoticonInteractionService, never()).handlePurchase(anyLong());
            }
        }

        @Test
        @DisplayName("기본 이모티콘")
        void purchaseEmoticon_DefaultEmoticon_Success() {
            // given
            Emoticon defaultEmoticon = createDefaultEmoticon();

            when(emoticonRepository.findById(200L)).thenReturn(Optional.of(defaultEmoticon));
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(USER_ID, 200L)).thenReturn(false);
            when(userEmoticonRepository.save(any(UserEmoticon.class))).thenReturn(mockUserEmoticon);

            // when
            EmoticonResponse response = emoticonService.purchaseEmoticon(USER_ID, 200L);

            // then
            assertThat(response).isNotNull();
            verify(userEmoticonRepository).save(any(UserEmoticon.class));
            verify(pointService, never()).usePoints(any(), any());
            verify(emoticonInteractionService).handlePurchase(200L);
        }

        private Emoticon createDefaultEmoticon() {
            Emoticon emoticon = mock(Emoticon.class);
            when(emoticon.getId()).thenReturn(200L);
            when(emoticon.getName()).thenReturn("Default Emoticon");
            when(emoticon.isDefault()).thenReturn(true);
            when(emoticon.getPrice()).thenReturn(0);
            return emoticon;
        }
    }

    @Nested
    @DisplayName("사용자 이모티콘 조회 테스트")
    class UserEmoticonTest {
        @Test
        @DisplayName("사용자 보유 이모티콘 조회")
        void getUserEmoticons_Success() {
            // given
            when(userEmoticonRepository.findByUserId(USER_ID)).thenReturn(List.of(mockUserEmoticon));

            // when
            UserEmoticonResponse response = emoticonService.getUserEmoticons(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOwnedEmoticons()).hasSize(1);
            assertThat(response.getNotOwnedEmoticons()).isEmpty();
            verify(userEmoticonRepository).findByUserId(USER_ID);
            verify(emoticonRepository).findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT);
        }

        @Test
        @DisplayName("사용 가능한 이모티콘 조회")
        void getAvailableEmoticons_Success() {
            // given
            when(userEmoticonRepository.findByUserId(USER_ID)).thenReturn(List.of(mockUserEmoticon));

            // when
            List<EmoticonResponse> responses = emoticonService.getAvailableEmoticons(USER_ID);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(EMOTICON_ID);
            verify(userEmoticonRepository).findByUserId(USER_ID);
        }

    }

    @Nested
    @DisplayName("이모티콘 업로드")
    class UploadEmoticonTest {

        @Test
        @DisplayName("이모티콘 업로드 성공")
        void uploadEmoticon_Success() throws IOException {
            // given
            EmoticonUploadRequest request = createUploadRequest();
            FileInfo fileInfo = createFileInfo();

            doNothing().when(fileStorageService).validateFile(mockFile);
            when(fileStorageService.storeFile(mockFile, EMOTICON_DIR)).thenReturn(fileInfo);
            when(emoticonRepository.save(any(Emoticon.class))).thenReturn(mockEmoticon);

            // when
            EmoticonResponse response = emoticonService.uploadEmoticon(mockFile, request, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(EMOTICON_ID);

            verify(fileStorageService).validateFile(mockFile);
            verify(fileStorageService).storeFile(mockFile, EMOTICON_DIR);
            verify(emoticonRepository).save(any(Emoticon.class));
            verify(slackNotifier).sendEmoticonUploadAlert(any(Emoticon.class), eq(mockUser));
        }

        @Test
        @DisplayName("이모티콘 업로드 실패 - 파일 저장 오류")
        void uploadEmoticon_FileStorageError_ThrowsException() throws IOException {
            // given
            EmoticonUploadRequest request = createUploadRequest();
            doNothing().when(fileStorageService).validateFile(mockFile);
            when(fileStorageService.storeFile(mockFile, EMOTICON_DIR))
                    .thenThrow(new IOException("File storage error"));

            // when & then
            assertThrows(IOException.class,
                    () -> emoticonService.uploadEmoticon(mockFile, request, USER_ID));

            verify(fileStorageService).validateFile(mockFile);
            verify(emoticonRepository, never()).save(any(Emoticon.class));
            verify(slackNotifier, never()).sendEmoticonUploadAlert(any(), any());
        }

        private EmoticonUploadRequest createUploadRequest() {
            return EmoticonUploadRequest.builder()
                    .name("New Emoticon")
                    .price(1500)
                    .build();
        }

        private FileInfo createFileInfo() {
            return FileInfo.builder()
                    .originalFileName("test.png")
                    .storedFileName("uuid.png")
                    .contentType("image/png")
                    .fileSize(1024L)
                    .build();
        }
    }

    @Nested
    @DisplayName("이모티콘 메시지 전송")
    class SendEmoticonMessageTest {
        @Test
        @DisplayName("이모티콘 메시지 전송 성공")
        void sendEmoticonMessage_Success() {
            // given
            EmoticonMessageRequest request = createMessageRequest();
            ChatRoom mockChatRoom = setupChatRoomMocks();
            ChatMessage mockCHatMessage = setupChatMessageMocks();

            when(userEmoticonRepository.existsByUserIdAndEmoticonId(USER_ID, EMOTICON_ID)).thenReturn(true);
            doNothing().when(chatRoomService).validateChatActivity(USER_ID, ROOM_ID);

            // when
            EmoticonMessageResponse result = emoticonService.sendEmoticonMessage(USER_ID, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmoticonId()).isEqualTo(EMOTICON_ID);
            assertThat(result.getRoomId()).isEqualTo(ROOM_ID);

            verify(chatMessageService).save(any(ChatMessage.class));
            verify(chatService).publishMessageEvent(any(), eq(OTHER_USER_ID), eq(true), eq("이모티콘을 전송했습니다"));
            verify(emoticonInteractionService).incrementUsage(EMOTICON_ID);
        }

        @Test
        @DisplayName("소유하지 않은 이모티콘 전송 실패")
        void sendEmoticonMessage_NotOwned_ThrowsException() {
            // given
            EmoticonMessageRequest request = createMessageRequest();
            ChatRoom mockChatRoom = mock(ChatRoom.class);

            when(chatRoomService.findChatRoomById(ROOM_ID)).thenReturn(mockChatRoom);
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(USER_ID, EMOTICON_ID)).thenReturn(false);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> emoticonService.sendEmoticonMessage(USER_ID, request));
            assertThat(exception.getErrorCode()).isEqualTo(EmoticonErrorCode.EMOTICON_PERMISSION_DENIED);
            verify(emoticonInteractionService, never()).incrementUsage(anyLong());
        }

        @Test
        @DisplayName("채팅방 권한 없음으로 전송 실패")
        void sendEmoticonMessage_NoRoomAccess_ThrowsException() {
            // given
            EmoticonMessageRequest request = createMessageRequest();
            ChatRoom mockChatRoom = mock(ChatRoom.class);

            when(chatRoomService.findChatRoomById(ROOM_ID)).thenReturn(mockChatRoom);
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(USER_ID, EMOTICON_ID)).thenReturn(true);
            doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                    .when(chatRoomService).validateChatActivity(USER_ID, ROOM_ID);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> emoticonService.sendEmoticonMessage(USER_ID, request));
            assertThat(exception.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    private EmoticonMessageRequest createMessageRequest() {
        return EmoticonMessageRequest.builder()
                .roomId(ROOM_ID)
                .emoticonId(EMOTICON_ID)
                .build();
    }

    private ChatRoom setupChatRoomMocks() {
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoomService.findChatRoomById(ROOM_ID)).thenReturn(chatRoom);
        when(chatRoom.isListener(mockUser)).thenReturn(true);
        when(chatRoom.getSpeaker()).thenReturn(mockOtherUser);
        when(chatRoom.getId()).thenReturn(ROOM_ID);
        doNothing().when(chatRoom).updateLastMessageTime();
        return chatRoom;
    }

    private ChatMessage setupChatMessageMocks() {
        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getId()).thenReturn(1L);
        when(chatMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(chatMessageService.save(any(ChatMessage.class))).thenReturn(chatMessage);
        when(chatPresenceService.isUserActiveInRoom(OTHER_USER_ID, ROOM_ID)).thenReturn(true);
        doNothing().when(chatService).publishMessageEvent(any(), eq(OTHER_USER_ID), eq(true), eq("이모티콘을 전송했습니다"));
        return chatMessage;
    }
}