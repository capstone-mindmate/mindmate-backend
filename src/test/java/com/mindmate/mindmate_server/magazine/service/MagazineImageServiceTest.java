package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
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
import java.util.List;
import java.util.Optional;

import static com.mindmate.mindmate_server.magazine.service.MagazineImageService.MAX_FILE_SIZE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineImageServiceTest {
    @Mock private MagazineImageRepository magazineImageRepository;

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

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.uploadImage(emptyFile));
            assertEquals(MagazineErrorCode.EMPTY_FILE, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미지 업로드 실패 - 잘못된 파일 ㅏ타입")
        void uploadImage_InvalidFileType() {
            // given
            MockMultipartFile textFile = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "test content".getBytes()
            );

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.uploadImage(textFile));
            assertEquals(MagazineErrorCode.INVALID_FILE_TYPE, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미지 업로드 실패 - 파일 크기 초과")
        void uploadImage_FileTooLarge() {
            // given
            byte[] largeContent = new byte[(int) (MAX_FILE_SIZE * 10)]; // 100MB

            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large/jpg",
                    "image/jpeg",
                    largeContent
            );

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
            List<MagazineImage> unusedImages = List.of(mockImage);
            when(magazineImageRepository.findByMagazineIsNullAndCreatedAtBefore(any(LocalDateTime.class)))
                    .thenReturn(unusedImages);

            // when
            magazineImageService.deleteUnusedImages();

            // then
            verify(magazineImageRepository).findByMagazineIsNullAndCreatedAtBefore(any(LocalDateTime.class));
            verify(magazineImageRepository).delete(mockImage);
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
            when(mockAuthor.getId()).thenReturn(userId);
            when(mockMagazine.getAuthor()).thenReturn(mockAuthor);
            when(mockImage.getMagazine()).thenReturn(mockMagazine);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineImageService.deleteImageById(unauthorizedUserID, imageId));
            assertEquals(MagazineErrorCode.MAGAZINE_IMAGE_ACCESS_DENIED, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미지 삭제 성공")
        void deleteImageById_Success() {
            // given
            Long userId = 1L;
            Magazine mockMagazine = mock(Magazine.class);
            User mockAuthor = mock(User.class);
            when(mockAuthor.getId()).thenReturn(userId);
            when(mockMagazine.getAuthor()).thenReturn(mockAuthor);
            when(mockImage.getMagazine()).thenReturn(mockMagazine);

            MagazineImageService spyService = spy(magazineImageService);
            doNothing().when(spyService).deleteImage(anyString());

            // when
            spyService.deleteImageById(userId, imageId);

            // then
            verify(spyService).deleteImage(mockImage.getStoredName());
            verify(magazineImageRepository).delete(mockImage);
        }

        @Test
        @DisplayName("연결된 매거진 없는 이미지 삭제 성공")
        void deleteImageById_NoMagazine_Success() {
            // given
            Long userId = 1L;
            when(mockImage.getMagazine()).thenReturn(null);

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