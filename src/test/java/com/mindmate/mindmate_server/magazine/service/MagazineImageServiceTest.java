package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineContent;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.dto.ImageResponse;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineImageRepository;
import com.mindmate.mindmate_server.user.domain.User;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mindmate.mindmate_server.magazine.service.MagazineImageService.MAX_FILE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineImageServiceTest {
    @Mock private MagazineImageRepository magazineImageRepository;
    @Mock private MagazineContentRepository magazineContentRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private MagazineImageService magazineImageService;

    private static final Long IMAGE_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long UNAUTHORIZED_USER_ID = 2L;
    private static final String ORIGINAL_NAME = "test.jpg";
    private static final String STORED_NAME = "uuid.webp";
    private static final String IMAGE_URL = "http://localhost:8080/images/uuid.webp";
    private static final String IMAGE_DIR = "src/test/resources/images/";
    private static final String IMAGE_URL_PREFIX = "http://localhost:8080/images/";

    private MagazineImage mockImage;
    private MockMultipartFile validFile;
    private FileInfo mockFileInfo;

    @BeforeEach
    void setup() {
        setupMockImage();
        setupMockFile();
        setupMockFileInfo();
        setupFieldInjection();
    }

    private void setupMockImage() {
        mockImage = mock(MagazineImage.class);
        when(mockImage.getId()).thenReturn(IMAGE_ID);
        when(mockImage.getOriginalName()).thenReturn(ORIGINAL_NAME);
        when(mockImage.getStoredName()).thenReturn(STORED_NAME);
        when(mockImage.getImageUrl()).thenReturn(IMAGE_URL);
        when(mockImage.getContentType()).thenReturn("image/webp");
        when(mockImage.getFileSize()).thenReturn(100L);

        when(magazineImageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(mockImage));
    }

    private void setupMockFile() {
        validFile = new MockMultipartFile(
                "image",
                ORIGINAL_NAME,
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    private void setupMockFileInfo() {
        mockFileInfo = mock(FileInfo.class);
        when(mockFileInfo.getOriginalFileName()).thenReturn(ORIGINAL_NAME);
        when(mockFileInfo.getStoredFileName()).thenReturn(STORED_NAME);
        when(mockFileInfo.getFileSize()).thenReturn(100L);
    }

    private void setupFieldInjection() {
        ReflectionTestUtils.setField(magazineImageService, "imageDir", IMAGE_DIR);
        ReflectionTestUtils.setField(magazineImageService, "imageUrlPrefix", IMAGE_URL_PREFIX);
    }

    @Nested
    @DisplayName("이미지 조회 테스트")
    class FindImageTest {
        @Test
        @DisplayName("이미지 ID로 조회 성공")
        void findMagazineImageById_Success() {
            // when
            MagazineImage result = magazineImageService.findMagazineImageById(IMAGE_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(IMAGE_ID);
            verify(magazineImageRepository).findById(IMAGE_ID);
        }

        @Test
        @DisplayName("이미지 ID로 조회 실패")
        void findMagazineImageById_NotFound() {
            // given
            Long nonExistImageId = 999L;
            when(magazineImageRepository.findById(nonExistImageId)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.findMagazineImageById(nonExistImageId));
            assertEquals(MagazineErrorCode.MAGAZINE_IMAGE_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("단일 이미지 업로드")
    class UploadSingleImageTest {
        @Test
        @DisplayName("이미지 업로드 성공")
        void uploadImage_Success() throws IOException {
            // given
            when(fileStorageService.storeFileAsWebp(validFile, IMAGE_DIR)).thenReturn(mockFileInfo);
            when(magazineImageRepository.save(any(MagazineImage.class))).thenReturn(mockImage);

            // when
            MagazineImage result = magazineImageService.uploadImage(validFile);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(IMAGE_ID);

            verify(fileStorageService).validateFile(validFile);
            verify(fileStorageService).storeFileAsWebp(validFile, IMAGE_DIR);
            verify(magazineImageRepository).save(any(MagazineImage.class));
        }

        @ParameterizedTest
        @DisplayName("이미지 업로드 실패 시나리오")
        @MethodSource("uploadFailureScenarios")
        void uploadImage_FailureScenarios(String description, MockMultipartFile file,
                                          Exception mockException, MagazineErrorCode expectedError) {
            // given
            if (mockException != null) {
                doThrow(mockException).when(fileStorageService).validateFile(file);
            }

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineImageService.uploadImage(file));
            assertThat(exception.getErrorCode()).isEqualTo(expectedError);
        }

        static Stream<Arguments> uploadFailureScenarios() {
            MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/jpeg", new byte[0]);
            MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
            MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg",
                    new byte[(int) (MAX_FILE_SIZE + 1)]);

            return Stream.of(
                    Arguments.of("빈 파일", emptyFile,
                            new CustomException(MagazineErrorCode.EMPTY_FILE), MagazineErrorCode.EMPTY_FILE),
                    Arguments.of("잘못된 파일 타입", textFile,
                            new CustomException(MagazineErrorCode.INVALID_FILE_TYPE), MagazineErrorCode.INVALID_FILE_TYPE),
                    Arguments.of("파일 크기 초과", largeFile, null, MagazineErrorCode.FILE_TOO_LARGE)
            );
        }
    }

    @Nested
    @DisplayName("다중 이미지 업로드")
    class UploadMultipleImagesTest {

        @Test
        @DisplayName("다중 이미지 업로드 성공")
        void uploadImages_Success() throws IOException {
            // given
            List<MultipartFile> files = List.of(validFile, validFile);
            when(fileStorageService.storeFileAsWebp(any(), eq(IMAGE_DIR))).thenReturn(mockFileInfo);
            when(magazineImageRepository.save(any(MagazineImage.class))).thenReturn(mockImage);

            // when
            List<ImageResponse> responses = magazineImageService.uploadImages(files);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getId()).isEqualTo(IMAGE_ID);
            assertThat(responses.get(0).getImageUrl()).isEqualTo(IMAGE_URL);

            verify(fileStorageService, times(2)).validateFile(any());
            verify(magazineImageRepository, times(2)).save(any(MagazineImage.class));
        }

        @Test
        @DisplayName("이미지 개수 초과 시 예외 발생")
        void uploadImages_TooManyImages_ThrowsException() {
            // given
            List<MultipartFile> tooManyFiles = IntStream.range(0, 11)
                    .mapToObj(i -> validFile)
                    .collect(Collectors.toList());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineImageService.uploadImages(tooManyFiles));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.TOO_MANY_IMAGES);
        }

        @Test
        @DisplayName("업로드 중 실패 시 롤백")
        void uploadImages_FailureRollback() throws IOException {
            // given
            List<MultipartFile> files = List.of(validFile, validFile);
            MagazineImageService spyService = spy(magazineImageService);

            when(fileStorageService.storeFileAsWebp(any(), eq(IMAGE_DIR))).thenReturn(mockFileInfo);
            when(magazineImageRepository.save(any(MagazineImage.class)))
                    .thenReturn(mockImage)
                    .thenThrow(new RuntimeException("Database error"));

            doNothing().when(spyService).deleteImage(anyString());

            // when & then
            assertThrows(RuntimeException.class, () -> spyService.uploadImages(files));

            verify(spyService).deleteImage(STORED_NAME);
            verify(magazineImageRepository).delete(mockImage);
        }
    }

    @Nested
    @DisplayName("미사용 이미지 삭제 테스트")
    class DeleteUnusedImagesTest {
        @Test
        @DisplayName("미사용 이미지 삭제 성공")
        void deleteUnusedImages_Success() {
            // given
            List<MagazineImage> candidates = List.of(mockImage);
            when(magazineImageRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                    .thenReturn(candidates);
            when(magazineContentRepository.existsByImage(mockImage)).thenReturn(false);

            MagazineImageService spyService = spy(magazineImageService);
            doNothing().when(spyService).deleteImage(anyString());

            // when
            spyService.deleteUnusedImages();

            // then
            verify(magazineImageRepository).findByCreatedAtBefore(any(LocalDateTime.class));
            verify(magazineContentRepository).existsByImage(mockImage);
            verify(spyService).deleteImage(mockImage.getStoredName());
            verify(magazineImageRepository).delete(mockImage);
        }

        @Test
        @DisplayName("사용 중인 이미지는 삭제하지 않음")
        void deleteUnusedImages_SkipUsedImages() {
            // given
            List<MagazineImage> candidates = List.of(mockImage);
            when(magazineImageRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                    .thenReturn(candidates);
            when(magazineContentRepository.existsByImage(mockImage)).thenReturn(true);

            MagazineImageService spyService = spy(magazineImageService);

            // when
            spyService.deleteUnusedImages();

            // then
            verify(magazineImageRepository).findByCreatedAtBefore(any(LocalDateTime.class));
            verify(magazineContentRepository).existsByImage(mockImage);
            verify(spyService, never()).deleteImage(anyString());
            verify(magazineImageRepository, never()).delete(any(MagazineImage.class));
        }
    }


    @Nested
    @DisplayName("이미지 삭제")
    class DeleteImageTest {

        @Test
        @DisplayName("권한이 있는 사용자의 이미지 삭제 성공")
        void deleteImageById_AuthorizedUser_Success() {
            // given
            setupAuthorizedUser();
            MagazineImageService spyService = spy(magazineImageService);
            doNothing().when(spyService).deleteImage(anyString());

            // when
            spyService.deleteImageById(USER_ID, IMAGE_ID);

            // then
            verify(spyService).deleteImage(STORED_NAME);
            verify(magazineImageRepository).delete(mockImage);
        }

        @Test
        @DisplayName("권한이 없는 사용자의 이미지 삭제 실패")
        void deleteImageById_UnauthorizedUser_ThrowsException() {
            // given
            setupAuthorizedUser();

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineImageService.deleteImageById(UNAUTHORIZED_USER_ID, IMAGE_ID));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.MAGAZINE_IMAGE_ACCESS_DENIED);
        }

        @Test
        @DisplayName("연결된 콘텐츠가 없는 이미지 삭제 성공")
        void deleteImageById_NoContent_Success() {
            // given
            when(magazineContentRepository.findByImage(mockImage)).thenReturn(Collections.emptyList());
            MagazineImageService spyService = spy(magazineImageService);
            doNothing().when(spyService).deleteImage(anyString());

            // when
            spyService.deleteImageById(USER_ID, IMAGE_ID);

            // then
            verify(spyService).deleteImage(STORED_NAME);
            verify(magazineImageRepository).delete(mockImage);
        }

        private void setupAuthorizedUser() {
            Magazine mockMagazine = mock(Magazine.class);
            User mockAuthor = mock(User.class);
            MagazineContent mockContent = mock(MagazineContent.class);

            when(mockAuthor.getId()).thenReturn(USER_ID);
            when(mockMagazine.getAuthor()).thenReturn(mockAuthor);
            when(mockContent.getMagazine()).thenReturn(mockMagazine);

            when(magazineContentRepository.findByImage(mockImage)).thenReturn(List.of(mockContent));
        }
    }

    @Test
    @DisplayName("물리적 파일 삭제")
    void deleteImage_FileSystemOperation() {
        // given
        String testFileName = "test-file.webp";

        // when
        magazineImageService.deleteImage(testFileName);

        // then
        assertDoesNotThrow(() -> magazineImageService.deleteImage(testFileName));
    }
}