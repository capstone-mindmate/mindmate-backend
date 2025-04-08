//package com.mindmate.mindmate_server.magazine;
//
//import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
//import com.mindmate.mindmate_server.magazine.repository.MagazineImageRepository;
//import com.mindmate.mindmate_server.magazine.service.MagazineImageService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class MagazineImageServiceIntegrationTest {
//    @Autowired
//    private MagazineImageService magazineImageService;
//
//    @Autowired
//    private MagazineImageRepository magazineImageRepository;
//
//    @Value("${image.test.dir}")
//    private String imageDir;
//
//    private final String TEST_IMAGE_PATH = "/test-image.jpg";
//
//    @BeforeEach
//    void setup() {
//        URL resource = getClass().getResource(TEST_IMAGE_PATH);
//        assertNotNull(resource, "테스트 이미지 파일이 존재하지 않습니다");
//
//        File directory = new File(imageDir);
//        if (!directory.exists()) {
//            directory.mkdirs();
//        }
//    }
//
//    @Test
//    @DisplayName("이미지 업로드 통합 테스트")
//    void uploadImage_Success() throws IOException {
//        // given
//        MockMultipartFile file = new MockMultipartFile(
//                "image",
//                "test.jpg",
//                "image/jpeg",
//                getClass().getResourceAsStream(TEST_IMAGE_PATH)
//        );
//
//        // when
//        MagazineImage result = magazineImageService.uploadImage(file);
//
//        try {
//            // then
//            assertNotNull(result);
//            assertNotNull(result.getId());
//            assertEquals("test.jpg", result.getOriginalName());
//            assertTrue(result.getImageUrl().contains(".webp"));
//
//            File savedFile = new File(imageDir, result.getStoredName());
//            assertTrue(savedFile.exists());
//        } finally {
//            if (result != null && result.getId() != null) {
//                magazineImageRepository.deleteById(result.getId());
//
//                File savedFile = new File(imageDir, result.getStoredName());
//                if (savedFile.exists()) {
//                    savedFile.delete();
//                }
//            }
//        }
//    }
//
//
//}