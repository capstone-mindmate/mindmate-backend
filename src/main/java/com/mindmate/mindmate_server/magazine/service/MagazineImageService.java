package com.mindmate.mindmate_server.magazine.service;

import com.luciad.imageio.webp.WebPWriteParam;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.dto.ImageResponse;
import com.mindmate.mindmate_server.magazine.repository.MagazineImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mindmate.mindmate_server.magazine.service.MagazineServiceImpl.MAX_IMAGE_SIZE;

@Service
@RequiredArgsConstructor
public class MagazineImageService {
    private final MagazineImageRepository magazineImageRepository;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 최대 파일 크기 10mb

    @Value("${image.dir}")
    private String imageDir;

    @Value("${image.url.prefix}")
    private String imageUrlPrefix;

    @Transactional
    public MagazineImage uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CustomException(MagazineErrorCode.EMPTY_FILE);
        }

        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(MagazineErrorCode.INVALID_FILE_TYPE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(MagazineErrorCode.FILE_TOO_LARGE);
        }

        String extension = "webp";
        String storedFileName = UUID.randomUUID() + "." + extension;

        File directory = new File(imageDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File destFile = new File(directory, storedFileName);

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();

        WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
        writeParam.setCompressionMode(WebPWriteParam.MODE_DEFAULT);

        writer.setOutput(new FileImageOutputStream(destFile));
        writer.write(null, new IIOImage(originalImage, null, null), writeParam);
        writer.dispose();

        String imageUrl = imageUrlPrefix + storedFileName;

        MagazineImage image = MagazineImage.builder()
                .originalName(originalFileName)
                .storedName(storedFileName)
                .imageUrl(imageUrl)
                .contentType("image/webp")
                .fileSize(destFile.length())
                .build();

        return magazineImageRepository.save(image);
    }

    @Transactional
    public List<ImageResponse> uploadImages(List<MultipartFile> files) throws IOException {
        if (files.size() > MAX_IMAGE_SIZE) {
            throw new CustomException(MagazineErrorCode.TOO_MANY_IMAGES);
        }
        List<ImageResponse> responses = new ArrayList<>();
        List<MagazineImage> savedImages = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                MagazineImage savedImage = uploadImage(file);
                savedImages.add(savedImage);
                responses.add(ImageResponse.builder()
                        .id(savedImage.getId())
                        .imageUrl(savedImage.getImageUrl())
                        .build());
            }

            return responses;
        } catch (Exception e) {
            for (MagazineImage image : savedImages) {
                deleteImage(image.getStoredName());
                magazineImageRepository.delete(image);
            }
            throw e;
        }


    }

    @Transactional
    public void deleteImage(String storedFileName) {
        File file = new File(imageDir, storedFileName);
        if (file.exists()) {
            file.delete();
        }
    }

    @Transactional
    public void deleteUnusedImages() {
        List<MagazineImage> unusedImages = magazineImageRepository.findByMagazineIsNullAndCreatedAtBefore(LocalDateTime.now().minusDays(1));

        for (MagazineImage image : unusedImages) {
            deleteImage(image.getStoredName());
            magazineImageRepository.delete(image);
        }
    }

    @Transactional
    public void deleteImageById(Long userId, Long imageId) {
        MagazineImage magazineImage = findMagazineImageById(imageId);

        if (magazineImage.getMagazine() != null) {
            if (!magazineImage.getMagazine().getAuthor().getId().equals(userId)) {
                throw new CustomException(MagazineErrorCode.MAGAZINE_IMAGE_ACCESS_DENIED);
            }
        }
        deleteImage(magazineImage.getStoredName());
        magazineImageRepository.delete(magazineImage);
    }

    public MagazineImage findMagazineImageById(Long imageId) {
        return magazineImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(MagazineErrorCode.MAGAZINE_IMAGE_NOT_FOUND));
    }
}
