package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineContent;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineImageRepository;
import com.mindmate.mindmate_server.user.domain.User;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.mindmate.mindmate_server.magazine.service.MagazineImageService.MAX_FILE_SIZE;
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

    private Long imageId;
    private MagazineImage mockImage;

    @BeforeEach
    void setup() {
        imageId = 1L;
        mockImage = mock(MagazineImage.class);

        when(mockImage.getId()).thenReturn(imageId);
        when(mockImage.getOriginalName()).thenReturn("test.jpg");
        when(mockImage.getStoredName()).thenReturn("uuid.webp");
        when(mockImage.getImageUrl()).thenReturn("test/images/uuid.webp");
        when(mockImage.getContentType()).thenReturn("image/webp");
        when(mockImage.getFileSize()).thenReturn(100L);

        when(magazineImageRepository.findById(imageId)).thenReturn(Optional.of(mockImage));

        // 동적 데이터 필드 주입
        ReflectionTestUtils.setField(magazineImageService, "imageDir", "src/test/resources/images/");
        ReflectionTestUtils.setField(magazineImageService, "imageUrlPrefix", "http://localhost:8080/images/");
    }

    @Nested
    @DisplayName("이미지 조회 테스트")
    class FindImageTest {
        @Test
        @DisplayName("이미지 ID로 조회 성공")
        void findMagazineImageById_Success() {
            // when
            MagazineImage result = magazineImageService.findMagazineImageById(imageId);

            // then
            assertNotNull(result);
            assertEquals(imageId, result.getId());
            verify(magazineImageRepository).findById(imageId);
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
    @DisplayName("이미지 업로드 유효성 검사 테스트")
    class UploadImageValidationTest {
        // 이미지 업로드 테스트 부분은 단위 테스트로 하기 매우매우 어려움
        // 외부 의존성을 다루고 있기 때문에 모킹이나 이런게 말이 안됨 -> 통합 테스트로 직접 테스트
        @Test
        @DisplayName("이미지 업로드 실패 - 빈 파일")
        void uploadImage_EmptyFile() {
            // given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "image",
                    "",
                    "image/jpeg",
                    new byte[0]
            );

            doThrow(new CustomException(MagazineErrorCode.EMPTY_FILE))
                    .when(fileStorageService).validateFile(emptyFile);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.uploadImage(emptyFile));
            assertEquals(MagazineErrorCode.EMPTY_FILE, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미지 업로드 실패 - 잘못된 파일 타입")
        void uploadImage_InvalidFileType() {
            // given
            MockMultipartFile textFile = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "test content".getBytes()
            );

            doThrow(new CustomException(MagazineErrorCode.INVALID_FILE_TYPE))
                    .when(fileStorageService).validateFile(textFile);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.uploadImage(textFile));
            assertEquals(MagazineErrorCode.INVALID_FILE_TYPE, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미지 업로드 실패 - 파일 크기 초과")
        void uploadImage_FileTooLarge() {
            // given
            byte[] largeContent = new byte[(int) (MagazineImageService.MAX_FILE_SIZE + 1)]; // 10MB 초과

            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large.jpg",
                    "image/jpeg",
                    largeContent
            );

            // fileStorageService.validateFile는 통과한다고 가정
            doNothing().when(fileStorageService).validateFile(largeFile);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.uploadImage(largeFile));
            assertEquals(MagazineErrorCode.FILE_TOO_LARGE, exception.getErrorCode());
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
    @DisplayName("이미지 삭제 테스트")
    class DeleteImageTest {
        @Test
        @DisplayName("이미지 삭제 권한 검사")
        void deleteImageById_AccessDenied() {
            // given
            Long userId = 1L;
            Long unauthorizedUserID = 2L;

            Magazine mockMagazine = mock(Magazine.class);
            User mockAuthor = mock(User.class);
            MagazineContent mockContent = mock(MagazineContent.class);

            when(mockAuthor.getId()).thenReturn(userId);
            when(mockMagazine.getAuthor()).thenReturn(mockAuthor);
            when(mockContent.getMagazine()).thenReturn(mockMagazine);

            List<MagazineContent> contents = List.of(mockContent);
            when(magazineContentRepository.findByImage(mockImage)).thenReturn(contents);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineImageService.deleteImageById(unauthorizedUserID, imageId));
            assertEquals(MagazineErrorCode.MAGAZINE_IMAGE_ACCESS_DENIED, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미지 삭제 성공 - 콘텐츠 연결된 경우")
        void deleteImageById_WithContent_Success() {
            // given
            Long userId = 1L;
            Magazine mockMagazine = mock(Magazine.class);
            User mockAuthor = mock(User.class);
            MagazineContent mockContent = mock(MagazineContent.class);

            when(mockAuthor.getId()).thenReturn(userId);
            when(mockMagazine.getAuthor()).thenReturn(mockAuthor);
            when(mockContent.getMagazine()).thenReturn(mockMagazine);

            List<MagazineContent> contents = List.of(mockContent);
            when(magazineContentRepository.findByImage(mockImage)).thenReturn(contents);

            MagazineImageService spyService = spy(magazineImageService);
            doNothing().when(spyService).deleteImage(anyString());

            // when
            spyService.deleteImageById(userId, imageId);

            // then
            verify(spyService).deleteImage(mockImage.getStoredName());
            verify(magazineImageRepository).delete(mockImage);
        }

        @Test
        @DisplayName("연결된 콘텐츠 없는 이미지 삭제 성공")
        void deleteImageById_NoContent_Success() {
            // given
            Long userId = 1L;
            when(magazineContentRepository.findByImage(mockImage)).thenReturn(Collections.emptyList());

            MagazineImageService spyService = spy(magazineImageService);
            doNothing().when(spyService).deleteImage(anyString());

            // when
            spyService.deleteImageById(userId, imageId);

            // then
            verify(spyService).deleteImage(mockImage.getStoredName());
            verify(magazineImageRepository).delete(mockImage);
        }
    }
}