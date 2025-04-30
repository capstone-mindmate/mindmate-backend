package com.mindmate.mindmate_server.global.service;

import com.luciad.imageio.webp.WebPWriteParam;
import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.exception.CommonErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    public FileInfo storeFile(MultipartFile file, String directory) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;

        File destDir = new File(directory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File destFile = new File(destDir, storedFileName);

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            ImageIO.write(originalImage, extension, destFile);

            return FileInfo.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .contentType(file.getContentType())
                    .fileSize(destFile.length())
                    .build();
        } catch (Exception e) {
            if (destFile.exists()) {
                destFile.delete();
            }
            throw e;
        }
    }

    public FileInfo storeFileAsWebp(MultipartFile file, String directory) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID() + ".webp";

        File destDir = new File(directory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File destFile = new File(destDir, storedFileName);

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();

            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            writeParam.setCompressionMode(WebPWriteParam.MODE_DEFAULT);

            writer.setOutput(new FileImageOutputStream(destFile));
            writer.write(null, new IIOImage(originalImage, null, null), writeParam);
            writer.dispose();

            return FileInfo.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .contentType("image/webp")
                    .fileSize(destFile.length())
                    .build();
        } catch (Exception e) {
            if (destFile.exists()) {
                destFile.delete();
            }
            throw e;
        }
    }


    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(CommonErrorCode.EMPTY_FILE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(CommonErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "png";
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) return "png";
        return filename.substring(lastDotIndex + 1);
    }
}