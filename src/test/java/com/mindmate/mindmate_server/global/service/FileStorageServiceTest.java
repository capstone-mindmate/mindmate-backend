package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.CommonErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock private MultipartFile mockFile;

    private final String TEST_DIRECTORY = "test-uploads";
    private final String TEST_ORIGINAL_FILENAME = "test-image.jpg";
    private final String TEST_CONTENT_TYPE = "image/jpeg";
    private final byte[] TEST_IMAGE_BYTES = createTestImageBytes();

    @BeforeEach
    void setUp() {
        setupMockFile();
        cleanupTestDirectory();
    }

    @AfterEach
    void tearDown() {
        cleanupTestDirectory();
    }

    private void setupMockFile() {
        when(mockFile.getOriginalFilename()).thenReturn(TEST_ORIGINAL_FILENAME);
        when(mockFile.getContentType()).thenReturn(TEST_CONTENT_TYPE);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) TEST_IMAGE_BYTES.length);
    }

    private void cleanupTestDirectory() {
        File testDir = new File(TEST_DIRECTORY);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            testDir.delete();
        }
    }

    private static byte[] createTestImageBytes() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("storeFile 테스트")
    class StoreFileTest {

        @Test
        @DisplayName("정상적인 이미지 파일 저장 성공")
        void storeFile_Success() throws IOException {
            // given
            InputStream inputStream = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when
            FileInfo result = fileStorageService.storeFile(mockFile, TEST_DIRECTORY);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalFileName()).isEqualTo(TEST_ORIGINAL_FILENAME);
            assertThat(result.getStoredFileName()).endsWith(".jpg");
            assertThat(result.getContentType()).isEqualTo(TEST_CONTENT_TYPE);
            assertThat(result.getFileSize()).isGreaterThan(0);

            File savedFile = new File(TEST_DIRECTORY, result.getStoredFileName());
            assertThat(savedFile).exists();
            assertThat(savedFile.length()).isEqualTo(result.getFileSize());
        }

        @ParameterizedTest(name = "[{index}] 파일명: {0}, 확장자: {1}")
        @MethodSource("fileExtensionScenarios")
        @DisplayName("다양한 파일 확장자 처리")
        void storeFile_VariousExtensions(String filename, String expectedExtension) throws IOException {
            // given
            when(mockFile.getOriginalFilename()).thenReturn(filename);
            InputStream inputStream = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when
            FileInfo result = fileStorageService.storeFile(mockFile, TEST_DIRECTORY);

            // then
            assertThat(result.getStoredFileName()).endsWith("." + expectedExtension);
        }

        static Stream<Arguments> fileExtensionScenarios() {
            return Stream.of(
                    Arguments.of("image.jpg", "jpg"),
                    Arguments.of("image.png", "png"),
                    Arguments.of("image.gif", "gif"),
                    Arguments.of("image.jpeg", "jpeg"),
                    Arguments.of("image", "png"),
                    Arguments.of(null, "png")
            );
        }

        @Test
        @DisplayName("디렉토리가 존재하지 않는 경우 자동 생성")
        void storeFile_CreateDirectoryIfNotExists() throws IOException {
            // given
            String nonExistentDir = "non-existent-dir/sub-dir";
            InputStream inputStream = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when
            FileInfo result = fileStorageService.storeFile(mockFile, nonExistentDir);

            // then
            File createdDir = new File(nonExistentDir);
            assertThat(createdDir).exists();
            assertThat(createdDir).isDirectory();

            File savedFile = new File(nonExistentDir, result.getStoredFileName());
            savedFile.delete();
            createdDir.delete();
            new File("non-existent-dir").delete();
        }

        @Test
        @DisplayName("파일 저장 중 예외 발생 시 생성된 파일 삭제")
        void storeFile_ExceptionHandling_DeletesFile() throws IOException {
            // given
            byte[] invalidImageData = "invalid image data".getBytes();
            InputStream inputStream = new ByteArrayInputStream(invalidImageData);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> fileStorageService.storeFile(mockFile, TEST_DIRECTORY));
        }


        @Test
        @DisplayName("UUID를 사용한 고유한 파일명 생성")
        void storeFile_GeneratesUniqueFilenames() throws IOException {
            // given
            InputStream inputStream1 = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            InputStream inputStream2 = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            when(mockFile.getInputStream()).thenReturn(inputStream1, inputStream2);

            // when
            FileInfo result1 = fileStorageService.storeFile(mockFile, TEST_DIRECTORY);
            FileInfo result2 = fileStorageService.storeFile(mockFile, TEST_DIRECTORY);

            // then
            assertThat(result1.getStoredFileName()).isNotEqualTo(result2.getStoredFileName());
            assertThat(result1.getStoredFileName()).matches("^[a-f0-9\\-]{36}\\.jpg$");
            assertThat(result2.getStoredFileName()).matches("^[a-f0-9\\-]{36}\\.jpg$");
        }
    }

    @Nested
    @DisplayName("storeFileAsWebp 테스트")
    class StoreFileAsWebpTest {

        @Test
        @DisplayName("이미지를 WebP 형식으로 변환하여 저장")
        void storeFileAsWebp_Success() throws IOException {
            // given
            InputStream inputStream = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when
            FileInfo result = fileStorageService.storeFileAsWebp(mockFile, TEST_DIRECTORY);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalFileName()).isEqualTo(TEST_ORIGINAL_FILENAME);
            assertThat(result.getStoredFileName()).endsWith(".webp");
            assertThat(result.getContentType()).isEqualTo("image/webp");
            assertThat(result.getFileSize()).isGreaterThan(0);

            File savedFile = new File(TEST_DIRECTORY, result.getStoredFileName());
            assertThat(savedFile).exists();
        }

        @Test
        @DisplayName("WebP 변환 중 예외 발생 시 파일 삭제")
        void storeFileAsWebp_ExceptionHandling() throws IOException {
            // given
            byte[] invalidImageData = "invalid image data".getBytes();
            InputStream inputStream = new ByteArrayInputStream(invalidImageData);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> fileStorageService.storeFileAsWebp(mockFile, TEST_DIRECTORY));
        }


        @Test
        @DisplayName("WebP 파일명이 UUID 형식으로 생성됨")
        void storeFileAsWebp_GeneratesUuidFilename() throws IOException {
            // given
            InputStream inputStream = new ByteArrayInputStream(TEST_IMAGE_BYTES);
            when(mockFile.getInputStream()).thenReturn(inputStream);

            // when
            FileInfo result = fileStorageService.storeFileAsWebp(mockFile, TEST_DIRECTORY);

            // then
            assertThat(result.getStoredFileName()).matches("^[a-f0-9\\-]{36}\\.webp$");
        }
    }

    @Nested
    @DisplayName("validateFile 테스트")
    class ValidateFileTest {

        @Test
        @DisplayName("정상적인 이미지 파일 검증 통과")
        void validateFile_ValidImageFile_Success() {
            // given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/jpeg");

            // when & then
            assertDoesNotThrow(() -> fileStorageService.validateFile(mockFile));
        }

        @ParameterizedTest(name = "[{index}] Content-Type: {0}")
        @ValueSource(strings = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"})
        @DisplayName("다양한 이미지 타입 검증 통과")
        void validateFile_VariousImageTypes_Success(String contentType) {
            // given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn(contentType);

            // when & then
            assertDoesNotThrow(() -> fileStorageService.validateFile(mockFile));
        }

        @Test
        @DisplayName("null 파일 검증 시 예외 발생")
        void validateFile_NullFile_ThrowsException() {
            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> fileStorageService.validateFile(null));

            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.EMPTY_FILE);
        }

        @Test
        @DisplayName("빈 파일 검증 시 예외 발생")
        void validateFile_EmptyFile_ThrowsException() {
            // given
            when(mockFile.isEmpty()).thenReturn(true);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> fileStorageService.validateFile(mockFile));

            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.EMPTY_FILE);
        }

        @ParameterizedTest(name = "[{index}] Content-Type: {0}")
        @ValueSource(strings = {"text/plain", "application/pdf", "video/mp4", "audio/mp3"})
        @DisplayName("이미지가 아닌 파일 타입 검증 시 예외 발생")
        void validateFile_NonImageType_ThrowsException(String contentType) {
            // given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn(contentType);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> fileStorageService.validateFile(mockFile));

            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_FILE_TYPE);
        }

        @Test
        @DisplayName("Content-Type이 null인 경우 예외 발생")
        void validateFile_NullContentType_ThrowsException() {
            // given
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn(null);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> fileStorageService.validateFile(mockFile));

            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_FILE_TYPE);
        }
    }

    @Nested
    @DisplayName("getExtension 테스트")
    class GetExtensionTest {

        @ParameterizedTest(name = "[{index}] 파일명: {0}, 예상 확장자: {1}")
        @MethodSource("extensionScenarios")
        @DisplayName("파일 확장자 추출 테스트")
        void getExtension_VariousFilenames(String filename, String expectedExtension) {
            // when
            String result = invokeGetExtension(filename);

            // then
            assertThat(result).isEqualTo(expectedExtension);
        }

        static Stream<Arguments> extensionScenarios() {
            return Stream.of(
                    Arguments.of("image.jpg", "jpg"),
                    Arguments.of("image.png", "png"),
                    Arguments.of("image.jpeg", "jpeg"),
                    Arguments.of("file.with.multiple.dots.gif", "gif"),
                    Arguments.of("noextension", "png"),
                    Arguments.of("", "png"),
                    Arguments.of(null, "png"),
                    Arguments.of("file.", ""),
                    Arguments.of(".hiddenfile", "hiddenfile")
            );
        }

        private String invokeGetExtension(String filename) {
            try {
                Method method = FileStorageService.class.getDeclaredMethod("getExtension", String.class);
                method.setAccessible(true);
                return (String) method.invoke(fileStorageService, filename);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
