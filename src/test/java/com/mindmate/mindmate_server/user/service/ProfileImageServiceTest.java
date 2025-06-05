package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.CommonErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.ProfileImage;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.ProfileImageResponse;
import com.mindmate.mindmate_server.user.repository.ProfileImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileImageServiceTest {

    @Mock private FileStorageService fileStorageService;
    @Mock private ProfileImageRepository profileImageRepository;
    @Mock private UserService userService;
    @Mock private MultipartFile mockFile;

    @InjectMocks
    private ProfileImageService profileImageService;

    private final Long TEST_USER_ID = 1L;
    private final Long TEST_IMAGE_ID = 10L;
    private final String TEST_PROFILE_DIR = "test-profiles/";
    private final String TEST_URL_PREFIX = "http://localhost:8080/images/";
    private final String TEST_DEFAULT_FILENAME = "default-profile.png";

    @BeforeEach
    void setUp() {
        setupTestProperties();
    }

    private void setupTestProperties() {
        ReflectionTestUtils.setField(profileImageService, "profileImageDir", TEST_PROFILE_DIR);
        ReflectionTestUtils.setField(profileImageService, "profileImageUrlPrefix", TEST_URL_PREFIX);
        ReflectionTestUtils.setField(profileImageService, "defaultProfileImageFilename", TEST_DEFAULT_FILENAME);
    }

    static class TestDataBuilder {
        static User createMockUser(Long userId) {
            User user = mock(User.class);
            Profile profile = mock(Profile.class);
            ProfileImage profileImage = mock(ProfileImage.class);

            when(user.getId()).thenReturn(userId);
            when(user.getProfile()).thenReturn(profile);
            when(profile.getProfileImage()).thenReturn(profileImage);

            return user;
        }

        static ProfileImage createMockProfileImage(Long id, Long userId, String storedName) {
            ProfileImage profileImage = mock(ProfileImage.class);
            User user = mock(User.class);
            when(user.getId()).thenReturn(userId);

            when(profileImage.getId()).thenReturn(id);
            when(profileImage.getUser()).thenReturn(user);
            when(profileImage.getStoredName()).thenReturn(storedName);
            when(profileImage.getOriginalName()).thenReturn("test-image.jpg");
            when(profileImage.getImageUrl()).thenReturn("http://localhost:8080/images/" + storedName);
            when(profileImage.getContentType()).thenReturn("image/webp");
            when(profileImage.getFileSize()).thenReturn(1024L);

            return profileImage;
        }

        static FileInfo createMockFileInfo(String originalName, String storedName) {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getOriginalFileName()).thenReturn(originalName);
            when(fileInfo.getStoredFileName()).thenReturn(storedName);
            when(fileInfo.getFileSize()).thenReturn(1024L);
            return fileInfo;
        }
    }

    @Nested
    @DisplayName("uploadProfileImage 테스트")
    class UploadProfileImageTest {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("uploadScenarios")
        @DisplayName("다양한 업로드 시나리오")
        void uploadProfileImage_VariousScenarios(
                String description,
                boolean hasExistingImage,
                boolean shouldThrowException,
                Class<? extends Exception> expectedExceptionType) throws IOException {

            // given
            User mockUser = TestDataBuilder.createMockUser(TEST_USER_ID);
            when(userService.findUserById(TEST_USER_ID)).thenReturn(mockUser);

            if (hasExistingImage) {
                ProfileImage existingImage = TestDataBuilder.createMockProfileImage(1L, TEST_USER_ID, "old-image.jpg");
                when(profileImageRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingImage));
            } else {
                when(profileImageRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
            }

            if (shouldThrowException) {
                when(fileStorageService.storeFile(mockFile, TEST_PROFILE_DIR))
                        .thenThrow(new IOException("Storage failed"));
            } else {
                FileInfo fileInfo = TestDataBuilder.createMockFileInfo("test.jpg", "uuid-test.jpg");
                when(fileStorageService.storeFile(mockFile, TEST_PROFILE_DIR)).thenReturn(fileInfo);

                ProfileImage savedImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, TEST_USER_ID, "uuid-test.jpg");
                when(profileImageRepository.save(any(ProfileImage.class))).thenReturn(savedImage);
            }

            // when & then
            if (shouldThrowException) {
                CustomException exception = assertThrows(CustomException.class,
                        () -> profileImageService.uploadProfileImage(TEST_USER_ID, mockFile));
                assertThat(exception.getErrorCode()).isEqualTo(ProfileErrorCode.IMAGE_UPLOAD_ERROR);
            } else {
                ProfileImageResponse result = profileImageService.uploadProfileImage(TEST_USER_ID, mockFile);

                assertThat(result).isNotNull();
                assertThat(result.getStoredFileName()).isEqualTo("uuid-test.jpg");
                assertThat(result.getContentType()).isEqualTo("image/webp");

                verify(fileStorageService).validateFile(mockFile);
                verify(profileImageRepository).save(any(ProfileImage.class));

                if (hasExistingImage) {
                    verify(profileImageRepository).delete(any(ProfileImage.class));
                }
            }
        }

        static Stream<Arguments> uploadScenarios() {
            return Stream.of(
                    Arguments.of("새 이미지 업로드 성공", false, false, null),
                    Arguments.of("기존 이미지 교체 성공", true, false, null),
                    Arguments.of("파일 저장 실패", false, true, CustomException.class)
            );
        }

        @Test
        @DisplayName("파일 검증 실패 시 예외 발생")
        void uploadProfileImage_FileValidationFails() throws IOException {
            // given
            doThrow(new CustomException(CommonErrorCode.INVALID_FILE_TYPE))
                    .when(fileStorageService).validateFile(mockFile);

            // when & then
            assertThrows(CustomException.class,
                    () -> profileImageService.uploadProfileImage(TEST_USER_ID, mockFile));

            verify(userService, never()).findUserById(anyLong());
            verify(profileImageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteProfileImage 테스트")
    class DeleteProfileImageTest {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("deleteScenarios")
        @DisplayName("다양한 삭제 시나리오")
        void deleteProfileImage_VariousScenarios(
                String description,
                boolean imageExists,
                boolean isOwner,
                ProfileErrorCode expectedErrorCode) {

            // given
            if (imageExists) {
                Long ownerId = isOwner ? TEST_USER_ID : 999L;
                ProfileImage profileImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, ownerId, "test.jpg");
                when(profileImageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(profileImage));
            } else {
                when(profileImageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.empty());
            }

            // when & then
            if (!imageExists || !isOwner) {
                CustomException exception = assertThrows(CustomException.class,
                        () -> profileImageService.deleteProfileImage(TEST_USER_ID, TEST_IMAGE_ID));
                assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
                verify(profileImageRepository, never()).delete(any());
            } else {
                profileImageService.deleteProfileImage(TEST_USER_ID, TEST_IMAGE_ID);
                verify(profileImageRepository).delete(any(ProfileImage.class));
            }
        }

        static Stream<Arguments> deleteScenarios() {
            return Stream.of(
                    Arguments.of("정상 삭제", true, true, null),
                    Arguments.of("이미지 없음", false, true, ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND),
                    Arguments.of("권한 없음", true, false, ProfileErrorCode.PROFILE_IMAGE_ACCESS_DENIED)
            );
        }
    }

    @Nested
    @DisplayName("findProfileImageByUserId 테스트")
    class FindProfileImageByUserIdTest {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("findByUserIdScenarios")
        @DisplayName("다양한 조회 시나리오")
        void findProfileImageByUserId_VariousScenarios(
                String description,
                boolean hasProfile,
                boolean hasProfileImage,
                boolean shouldThrowException) {

            // given
            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(TEST_USER_ID);
            when(userService.findUserById(TEST_USER_ID)).thenReturn(mockUser);

            if (hasProfile) {
                Profile mockProfile = mock(Profile.class);
                when(mockUser.getProfile()).thenReturn(mockProfile);

                if (hasProfileImage) {
                    ProfileImage mockProfileImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, TEST_USER_ID, "test.jpg");
                    when(mockProfile.getProfileImage()).thenReturn(mockProfileImage);
                } else {
                    when(mockProfile.getProfileImage()).thenReturn(null);
                }
            } else {
                when(mockUser.getProfile()).thenReturn(null);
            }

            // when & then
            if (shouldThrowException) {
                CustomException exception = assertThrows(CustomException.class,
                        () -> profileImageService.findProfileImageByUserId(TEST_USER_ID));
                assertThat(exception.getErrorCode()).isEqualTo(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND);
            } else {
                ProfileImage result = profileImageService.findProfileImageByUserId(TEST_USER_ID);
                assertThat(result).isNotNull();
            }
        }

        static Stream<Arguments> findByUserIdScenarios() {
            return Stream.of(
                    Arguments.of("정상 조회", true, true, false),
                    Arguments.of("프로필 없음", false, false, true),
                    Arguments.of("프로필 이미지 없음", true, false, true)
            );
        }
    }

    @Nested
    @DisplayName("findProfileImageById 테스트")
    class FindProfileImageByIdTest {

        @ParameterizedTest(name = "[{index}] 이미지 존재: {0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("ID로 프로필 이미지 조회")
        void findProfileImageById_VariousScenarios(boolean imageExists) {
            // given
            if (imageExists) {
                ProfileImage mockProfileImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, TEST_USER_ID, "test.jpg");
                when(profileImageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(mockProfileImage));
            } else {
                when(profileImageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.empty());
            }

            // when & then
            if (imageExists) {
                ProfileImage result = profileImageService.findProfileImageById(TEST_IMAGE_ID);
                assertThat(result).isNotNull();
            } else {
                CustomException exception = assertThrows(CustomException.class,
                        () -> profileImageService.findProfileImageById(TEST_IMAGE_ID));
                assertThat(exception.getErrorCode()).isEqualTo(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getDefaultProfileImage 테스트")
    class GetDefaultProfileImageTest {

        @ParameterizedTest(name = "[{index}] 기본 이미지 존재: {0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("기본 프로필 이미지 조회")
        void getDefaultProfileImage_VariousScenarios(boolean defaultImageExists) {
            // given
            if (defaultImageExists) {
                ProfileImage mockProfileImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, null, TEST_DEFAULT_FILENAME);
                when(profileImageRepository.findByOriginalNameAndUserIsNull(TEST_DEFAULT_FILENAME))
                        .thenReturn(Optional.of(mockProfileImage));
            } else {
                when(profileImageRepository.findByOriginalNameAndUserIsNull(TEST_DEFAULT_FILENAME))
                        .thenReturn(Optional.empty());
            }

            // when & then
            if (defaultImageExists) {
                ProfileImage result = profileImageService.getDefaultProfileImage();
                assertThat(result).isNotNull();
            } else {
                CustomException exception = assertThrows(CustomException.class,
                        () -> profileImageService.getDefaultProfileImage());
                assertThat(exception.getErrorCode()).isEqualTo(ProfileErrorCode.PROFILE_IMAGE_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("registerDefaultProfileImage 테스트")
    class RegisterDefaultProfileImageTest {

        @Test
        @DisplayName("기본 프로필 이미지가 이미 존재하는 경우")
        void registerDefaultProfileImage_AlreadyExists() {
            // given
            ProfileImage existingImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, null, TEST_DEFAULT_FILENAME);
            when(profileImageRepository.findByOriginalNameAndUserIsNull(TEST_DEFAULT_FILENAME))
                    .thenReturn(Optional.of(existingImage));

            ProfileImageResponse expectedResponse = ProfileImageResponse.builder()
                    .originalFileName(TEST_DEFAULT_FILENAME)
                    .build();

            try (MockedStatic<ProfileImageResponse> mockedResponse = mockStatic(ProfileImageResponse.class)) {
                mockedResponse.when(() -> ProfileImageResponse.from(existingImage))
                        .thenReturn(expectedResponse);

                // when
                ProfileImageResponse result = profileImageService.registerDefaultProfileImage();

                // then
                assertThat(result).isEqualTo(expectedResponse);
                verify(profileImageRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("새로운 기본 프로필 이미지 등록 성공")
        void registerDefaultProfileImage_NewRegistration_Success() throws IOException {
            // given
            when(profileImageRepository.findByOriginalNameAndUserIsNull(TEST_DEFAULT_FILENAME))
                    .thenReturn(Optional.empty());

            // 실제 테스트용 파일 생성
            String testDir = "test-default/";
            File testDirectory = new File(testDir);
            testDirectory.mkdirs();
            File testFile = new File(testDir + TEST_DEFAULT_FILENAME);
            testFile.createNewFile();

            ReflectionTestUtils.setField(profileImageService, "profileImageDir", testDir);

            ProfileImage savedImage = TestDataBuilder.createMockProfileImage(TEST_IMAGE_ID, null, TEST_DEFAULT_FILENAME);
            when(profileImageRepository.save(any(ProfileImage.class))).thenReturn(savedImage);

            ProfileImageResponse expectedResponse = ProfileImageResponse.builder()
                    .id(TEST_IMAGE_ID)
                    .originalFileName(TEST_DEFAULT_FILENAME)
                    .storedFileName(TEST_DEFAULT_FILENAME)
                    .imageUrl(TEST_URL_PREFIX + TEST_DEFAULT_FILENAME)
                    .contentType("image/png")
                    .fileSize(testFile.length())
                    .build();

            try (MockedStatic<ProfileImageResponse> mockedResponse = mockStatic(ProfileImageResponse.class)) {
                mockedResponse.when(() -> ProfileImageResponse.from(savedImage))
                        .thenReturn(expectedResponse);

                // when
                ProfileImageResponse result = profileImageService.registerDefaultProfileImage();

                // then
                assertThat(result).isEqualTo(expectedResponse);
                verify(profileImageRepository).save(any(ProfileImage.class));

            } finally {
                testFile.delete();
                testDirectory.delete();
            }
        }

        @Test
        @DisplayName("기본 프로필 이미지 파일이 존재하지 않는 경우")
        void registerDefaultProfileImage_FileNotExists() {
            // given
            when(profileImageRepository.findByOriginalNameAndUserIsNull(TEST_DEFAULT_FILENAME))
                    .thenReturn(Optional.empty());

            ReflectionTestUtils.setField(profileImageService, "profileImageDir", "non-existent-dir/");

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> profileImageService.registerDefaultProfileImage());
            assertThat(exception.getErrorCode()).isEqualTo(ProfileErrorCode.IMAGE_UPLOAD_ERROR);
        }
    }


}
