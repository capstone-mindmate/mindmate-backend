package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonType;
import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonDetailResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.dto.UserEmoticonResponse;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.emoticon.repository.UserEmoticonRepository;
import com.mindmate.mindmate_server.global.dto.FileInfo;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    private EmoticonServiceImpl emoticonService;

    private Long userId;
    private Long emoticonId;
    private User mockUser;
    private Emoticon mockEmoticon;
    private UserEmoticon mockUserEmoticon;
    private MultipartFile mockFile;

    @BeforeEach
    void setup() {
        userId = 1L;
        emoticonId = 100L;

        mockUser = mock(User.class);
        mockEmoticon = mock(Emoticon.class);
        mockUserEmoticon = mock(UserEmoticon.class);
        mockFile = mock(MultipartFile.class);

        Profile mockProfile = mock(Profile.class);
        when(mockProfile.getNickname()).thenReturn("testUser");
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getProfile()).thenReturn(mockProfile);

        when(mockEmoticon.getId()).thenReturn(emoticonId);
        when(mockEmoticon.getName()).thenReturn("Test Emoticon");
        when(mockEmoticon.getImageUrl()).thenReturn("test/emoticons/uuid.png");
        when(mockEmoticon.getPrice()).thenReturn(1000);
        when(mockEmoticon.isDefault()).thenReturn(false);
        when(mockEmoticon.getStatus()).thenReturn(EmoticonStatus.ACCEPT);
        when(mockEmoticon.getCreator()).thenReturn(mockUser);

        when(mockUserEmoticon.getUser()).thenReturn(mockUser);
        when(mockUserEmoticon.getEmoticon()).thenReturn(mockEmoticon);
        when(mockUserEmoticon.getType()).thenReturn(EmoticonType.CREATED);

        when(emoticonRepository.findById(emoticonId)).thenReturn(Optional.of(mockEmoticon));
        when(userService.findUserById(userId)).thenReturn(mockUser);

        ReflectionTestUtils.setField(emoticonService, "emoticonDir", "./uploads/emoticons/");
        ReflectionTestUtils.setField(emoticonService, "emoticonUrlPrefix", "/emoticons/");
    }

    @Nested
    @DisplayName("이모티콘 샾 조회 테스트")
    class GetShopEmoticonsTest {
        @ParameterizedTest
        @DisplayName("이모티콘 샾 조회 테스트")
        @MethodSource("shopEmoticonTestCases")
        void getShopEmoticons_VariousStates(Long testUserId, boolean expectedPurchased) {
            // given
            List<Emoticon> emoticons = List.of(mockEmoticon);
            when(emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT)).thenReturn(emoticons);

            if (testUserId != null) {
                when(userEmoticonRepository.findByUserId(testUserId)).thenReturn(testUserId.equals(userId) ? List.of(mockUserEmoticon) : Collections.emptyList());
            }

            // when
            List<EmoticonResponse> responses = emoticonService.getShopEmoticons(testUserId);

            // then
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(emoticonId, responses.get(0).getId());
            assertEquals(expectedPurchased, responses.get(0).isPurchased());
        }

        static Stream<Arguments> shopEmoticonTestCases() {
            return Stream.of(
                    Arguments.of(1L, true),
                    Arguments.of(2L, false),
                    Arguments.of(null, false)
            );
        }
    }

    @Nested
    @DisplayName("이모티콘 상세 조회 테스트")
    class GetEmoticonDetailTest {
        @ParameterizedTest
        @DisplayName("이모티콘 상세 조회 성공")
        @MethodSource("emoticonDetailTestCases")
        void getEmoticonDetail_Success(Long testUserId, boolean hasPurchased) {
            // given
            if (testUserId != null) {
                when(userEmoticonRepository.existsByUserIdAndEmoticonId(testUserId, emoticonId)).thenReturn(hasPurchased);
            }

            List<Emoticon> similarEmoticons = new ArrayList<>();
            similarEmoticons.add(mockEmoticon);
            when(emoticonRepository.findByStatusAndIsDefaultOrderByCreatedAtDesc(EmoticonStatus.ACCEPT, false)).thenReturn(similarEmoticons);

            // when
            EmoticonDetailResponse response = emoticonService.getEmoticonDetail(emoticonId, testUserId);

            // then
            assertNotNull(response);
            assertEquals(emoticonId, response.getEmoticon().getId());
            assertEquals(hasPurchased, response.getEmoticon().isPurchased());
        }

        static Stream<Arguments> emoticonDetailTestCases() {
            return Stream.of(
                    Arguments.of(1L, true),
                    Arguments.of(1L, false),
                    Arguments.of(null, false)
            );
        }

        @Test
        @DisplayName("이모티콘 상세 조회 실패 - 존재하지 않는 이모티콘")
        void getEmoticonDetail_NotFound() {
            // given
            when(emoticonRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> emoticonService.getEmoticonDetail(999L, userId));
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
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(userId, emoticonId))
                    .thenReturn(alreadyPurchased);
            when(pointService.getCurrentBalance(userId)).thenReturn(userPoints);
            when(emoticonRepository.save(any(Emoticon.class))).thenReturn(mockEmoticon);

            // when & then
            if (shouldSucceed) {
                EmoticonResponse response = emoticonService.purchaseEmoticon(userId, emoticonId);
                assertNotNull(response);
                assertEquals(emoticonId, response.getId());
                verify(userEmoticonRepository).save(any(UserEmoticon.class));
            } else {
                assertThrows(CustomException.class, () -> emoticonService.purchaseEmoticon(userId, emoticonId));
            }
        }

        @Test
        @DisplayName("기본 이모티콘")
        void purchaseEmoticon_DefaultEmoticon_Success() {
            // given
            Emoticon defaultEmoticon = mock(Emoticon.class);
            when(defaultEmoticon.getId()).thenReturn(200L);
            when(defaultEmoticon.getName()).thenReturn("Default Emoticon");
            when(defaultEmoticon.isDefault()).thenReturn(true);

            when(emoticonRepository.findById(200L)).thenReturn(Optional.of(defaultEmoticon));
            when(userEmoticonRepository.existsByUserIdAndEmoticonId(userId, 200L)).thenReturn(false);

            // when
            EmoticonResponse response = emoticonService.purchaseEmoticon(userId, 200L);

            // then
            assertNotNull(response);
            verify(userEmoticonRepository).save(any(UserEmoticon.class));
            verify(pointService, never()).usePoints(any(), any());
        }
    }

    @Nested
    @DisplayName("사용자 이모티콘 조회 테스트")
    class UserEmoticonTest {
        @Test
        @DisplayName("사용자 보유 이모티콘 조회")
        void getUserEmoticons_Success() {
            // given
            List<UserEmoticon> userEmoticons = List.of(mockUserEmoticon);
            List<Emoticon> allEmoticons = List.of(mockEmoticon);

            when(userEmoticonRepository.findByUserId(userId)).thenReturn(userEmoticons);
            when(emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT))
                    .thenReturn(allEmoticons);

            // when
            UserEmoticonResponse response = emoticonService.getUserEmoticons(userId);

            // then
            assertNotNull(response);
            assertEquals(1, response.getOwnedEmoticons().size());
            assertEquals(0, response.getNotOwnedEmoticons().size());
            verify(userEmoticonRepository).findByUserId(userId);

            verify(emoticonRepository).findByStatusOrderByCreatedAtDesc(EmoticonStatus.ACCEPT);
        }

        @Test
        @DisplayName("사용 가능한 이모티콘 조회")
        void getAvailableEmoticons_Success() {
            // given
            List<UserEmoticon> userEmoticons = List.of(mockUserEmoticon);
            when(userEmoticonRepository.findByUserId(userId)).thenReturn(userEmoticons);

            // when
            List<EmoticonResponse> responses = emoticonService.getAvailableEmoticons(userId);

            // then
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(emoticonId, responses.get(0).getId());
            verify(userEmoticonRepository).findByUserId(userId);
        }

    }

    @Nested
    @DisplayName("이모티콘 업로드 테스트")
    class UploadEmoticonTest {
        @Test
        @DisplayName("이모티콘 업로드 성공")
        void uploadEmoticon_Success() throws IOException {
            // given
            String testEmoticonDir = "./uploads/emoticons/";
            EmoticonUploadRequest request = EmoticonUploadRequest.builder()
                    .name("New Emoticon")
                    .price(1500L)
                    .build();

            FileInfo fileInfo = FileInfo.builder()
                    .originalFileName("test.png")
                    .storedFileName("uuid.png")
                    .contentType("image/png")
                    .fileSize(1024L)
                    .build();

            doNothing().when(fileStorageService).validateFile(mockFile);
            when(fileStorageService.storeFile(any(MultipartFile.class), anyString())).thenReturn(fileInfo);
            when(emoticonRepository.save(any(Emoticon.class))).thenReturn(mockEmoticon);

            // when
            EmoticonResponse response = emoticonService.uploadEmoticon(mockFile, request, userId);

            // then
            assertNotNull(response);
            assertEquals(emoticonId, response.getId());
            verify(fileStorageService).validateFile(mockFile);
            verify(fileStorageService).storeFile(mockFile, testEmoticonDir);
            verify(emoticonRepository).save(any(Emoticon.class));
            verify(slackNotifier).sendEmoticonUploadAlert(any(Emoticon.class), any(User.class));
        }

        @Test
        @DisplayName("이모티콘 업로드 실패 - 파일 저장 오류")
        void uploadEmoticon_FileStorageError() throws IOException {
            // given
            EmoticonUploadRequest request = EmoticonUploadRequest.builder()
                    .name("New Emoticon")
                    .price(1500L)
                    .build();
            when(fileStorageService.storeFile(any(MultipartFile.class), anyString()))
                    .thenThrow(new IOException("File storage error"));

            // when & then
            assertThrows(IOException.class, () -> emoticonService.uploadEmoticon(mockFile, request, userId));
            verify(fileStorageService).validateFile(mockFile);
            verify(emoticonRepository, never()).save(any(Emoticon.class));
        }
    }


}