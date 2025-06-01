package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import com.mindmate.mindmate_server.emoticon.dto.BulkEmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonAdminResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonNotificationEvent;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.emoticon.repository.UserEmoticonRepository;
import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminEmoticonServiceTest {
    @Mock private EmoticonRepository emoticonRepository;
    @Mock private UserEmoticonRepository userEmoticonRepository;
    @Mock private EmoticonService emoticonService;
    @Mock private NotificationService notificationService;
    @Mock private UserService userService;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private AdminEmoticonService adminEmoticonService;

    private Long userId;
    private Long emoticonId;
    private User mockUser;
    private Emoticon mockEmoticon;
    private Emoticon noCreatorEmoticon;

    @BeforeEach
    void setup() {
        userId = 1L;
        emoticonId = 100L;

        mockUser = mock(User.class);
        mockEmoticon = mock(Emoticon.class);
        noCreatorEmoticon = mock(Emoticon.class);

        Profile mockProfile = mock(Profile.class);
        when(mockProfile.getNickname()).thenReturn("testUser");
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getProfile()).thenReturn(mockProfile);

        when(mockEmoticon.getId()).thenReturn(emoticonId);
        when(mockEmoticon.getName()).thenReturn("Test Emoticon");
        when(mockEmoticon.getImageUrl()).thenReturn("test/emoticons/uuid.png");
        when(mockEmoticon.getPrice()).thenReturn(1000);
        when(mockEmoticon.isDefault()).thenReturn(false);
        when(mockEmoticon.getStatus()).thenReturn(EmoticonStatus.PENDING);
        when(mockEmoticon.getCreator()).thenReturn(mockUser);

        when(noCreatorEmoticon.getId()).thenReturn(200L);
        when(noCreatorEmoticon.getName()).thenReturn("No Creator Emoticon");
        when(noCreatorEmoticon.getStatus()).thenReturn(EmoticonStatus.PENDING);
        when(noCreatorEmoticon.getCreator()).thenReturn(null);

        when(emoticonService.findEmoticonById(emoticonId)).thenReturn(mockEmoticon);
        when(emoticonService.findEmoticonById(200L)).thenReturn(noCreatorEmoticon);

        doNothing().when(notificationService).processNotification(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("대기 중인 이모티콘 목록 조회")
    void getPendingEmoticons_Success() {
        // given
        List<Emoticon> pendingEmoticons = List.of(mockEmoticon);
        when(emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.PENDING))
                .thenReturn(pendingEmoticons);

        // when
        List<EmoticonAdminResponse> responses = adminEmoticonService.getPendingEmoticons();

        // then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(emoticonId, responses.get(0).getId());
        assertEquals("Test Emoticon", responses.get(0).getName());

        verify(emoticonRepository).findByStatusOrderByCreatedAtDesc(EmoticonStatus.PENDING);
    }

    @ParameterizedTest
    @DisplayName("이모티콘 승인 시나리오")
    @MethodSource("emoticonApprovalTestCases")
    void acceptEmoticon_Scenarios(
            Long testEmoticonId,
            boolean hasCreator,
            boolean alreadyOwned,
            boolean shouldSaveUserEmoticon,
            boolean shouldSendNotification) {
        // given
        if (hasCreator) {
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(userId, testEmoticonId))
                    .thenReturn(alreadyOwned);
        }

        // when
        adminEmoticonService.acceptEmoticon(testEmoticonId);

        // then
        Emoticon targetEmoticon = testEmoticonId.equals(emoticonId) ? mockEmoticon : noCreatorEmoticon;

        verify(targetEmoticon).updateStatus(EmoticonStatus.ACCEPT);
        verify(emoticonRepository).save(targetEmoticon);

        if (shouldSaveUserEmoticon) {
            verify(userEmoticonRepository).save(any(UserEmoticon.class));
        } else {
            verify(userEmoticonRepository, never()).save(any(UserEmoticon.class));
        }
    }

    @ParameterizedTest
    @DisplayName("이모티콘 거절 시나리오")
    @MethodSource("emoticonRejectionTestCases")
    void rejectEmoticon_Scenarios(
            Long testEmoticonId,
            boolean hasCreator,
            boolean shouldSendNotification) {
        // when
        adminEmoticonService.rejectEmoticon(testEmoticonId);

        // then
        Emoticon targetEmoticon = testEmoticonId.equals(emoticonId) ? mockEmoticon : noCreatorEmoticon;

        verify(targetEmoticon).updateStatus(EmoticonStatus.REJECT);
        verify(emoticonRepository).save(targetEmoticon);

        if (shouldSendNotification) {
            verify(notificationService).processNotification(any(EmoticonNotificationEvent.class));
        } else {
            verify(notificationService, never()).processNotification(any());
        }
    }

    static Stream<Arguments> emoticonApprovalTestCases() {
        return Stream.of(
                Arguments.of(100L, true, false, true, true), // 제작자 있음, 미소유, UserEmoticon 저장 O, 알림 O
                Arguments.of(100L, true, true, false, true), // 제작자 있음, 이미 소유, UserEmoticon 저장 X, 알림 O
                Arguments.of(200L, false, false, false, false) // 제작자 없음, 미소유, UserEmoticon 저장 X, 알림 X
        );
    }

    static Stream<Arguments> emoticonRejectionTestCases() {
        return Stream.of(
                Arguments.of(100L, true, true), // 제작자 있음, 알림 O
                Arguments.of(200L, false, false) // 제작자 없음, 알림 X
        );
    }

    @Nested
    @DisplayName("대량 이모티콘 업로드 테스트")
    class BulkUploadTest {
        @Test
        @DisplayName("대량 이모티콘 업로드 성공")
        void uploadBulkEmoticons_Success() throws IOException {
            // given
            MockMultipartFile file1 = new MockMultipartFile("file1", "test1.png", "image/png", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("file2", "test2.png", "image/png", "content2".getBytes());
            MultipartFile[] files = {file1, file2};

            List<EmoticonUploadRequest> emoticons = List.of(
                    EmoticonUploadRequest.builder().name("Emoticon 1").price(100).build(),
                    EmoticonUploadRequest.builder().name("Emoticon 2").price(200).build()
            );

            BulkEmoticonUploadRequest request = BulkEmoticonUploadRequest.builder()
                    .emoticons(emoticons)
                    .isDefault(false)
                    .build();

            FileInfo fileInfo1 = FileInfo.builder()
                    .originalFileName("test1.png")
                    .storedFileName("stored1.png")
                    .contentType("image/png")
                    .fileSize(1024L)
                    .build();

            FileInfo fileInfo2 = FileInfo.builder()
                    .originalFileName("test2.png")
                    .storedFileName("stored2.png")
                    .contentType("image/png")
                    .fileSize(2048L)
                    .build();

            when(userService.findUserById(userId)).thenReturn(mockUser);
            doNothing().when(fileStorageService).validateFile(any());
            when(fileStorageService.storeFile(file1, "emoticon-dir")).thenReturn(fileInfo1);
            when(fileStorageService.storeFile(file2, "emoticon-dir")).thenReturn(fileInfo2);
            when(emoticonRepository.save(any(Emoticon.class))).thenReturn(mockEmoticon);

            ReflectionTestUtils.setField(adminEmoticonService, "emoticonDir", "emoticon-dir");
            ReflectionTestUtils.setField(adminEmoticonService, "emoticonUrlPrefix", "http://test.com/");

            // when
            List<EmoticonAdminResponse> result = adminEmoticonService.uploadBulkEmoticons(userId, files, request);

            // then
            assertThat(result).hasSize(2);
            verify(fileStorageService, times(2)).validateFile(any());
            verify(emoticonRepository, times(2)).save(any(Emoticon.class));
            verify(userEmoticonRepository, times(2)).save(any(UserEmoticon.class));
        }
    }

}