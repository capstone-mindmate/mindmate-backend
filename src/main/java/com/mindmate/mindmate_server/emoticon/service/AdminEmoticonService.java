package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonType;
import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import com.mindmate.mindmate_server.emoticon.dto.BulkEmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonAdminResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonNotificationEvent;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.emoticon.repository.UserEmoticonRepository;
import com.mindmate.mindmate_server.global.dto.FileInfo;
import com.mindmate.mindmate_server.global.service.FileStorageService;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminEmoticonService {
    private final EmoticonRepository emoticonRepository;
    private final UserEmoticonRepository userEmoticonRepository;

    private final EmoticonService emoticonService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final UserService userService;


    @Value("${emoticon.dir}")
    private String emoticonDir;

    @Value("${emoticon.url.prefix}")
    private String emoticonUrlPrefix;

    public List<EmoticonAdminResponse> getPendingEmoticons() {
        List<Emoticon> pendingEmoticons = emoticonRepository.findByStatusOrderByCreatedAtDesc(EmoticonStatus.PENDING);

        return pendingEmoticons.stream()
                .map(emoticon -> EmoticonAdminResponse.builder()
                        .id(emoticon.getId())
                        .name(emoticon.getName())
                        .imageUrl(emoticon.getImageUrl())
                        .price(emoticon.getPrice())
                        .contentType(emoticon.getContentType())
                        .fileSize(emoticon.getFileSize())
                        .createdAt(emoticon.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptEmoticon(Long emoticonId) {
        Emoticon emoticon = emoticonService.findEmoticonById(emoticonId);
        emoticon.updateStatus(EmoticonStatus.ACCEPT);
        emoticonRepository.save(emoticon);

        if (emoticon.getCreator() != null) {
            User creator = emoticon.getCreator();

            if (!userEmoticonRepository.existsByUserIdAndEmoticonId(creator.getId(), emoticonId)) {
                UserEmoticon userEmoticon = UserEmoticon.builder()
                        .user(creator)
                        .emoticon(emoticon)
                        .type(EmoticonType.CREATED)
                        .purchasePrice(0)
                        .build();

                userEmoticonRepository.save(userEmoticon);
                notifyCreator(emoticon, EmoticonStatus.ACCEPT);
            }
        }
    }

    @Transactional
    public void rejectEmoticon(Long emoticonId) {
        Emoticon emoticon = emoticonService.findEmoticonById(emoticonId);
        emoticon.updateStatus(EmoticonStatus.REJECT);
        emoticonRepository.save(emoticon);
        // todo: reject 후의 이모티콘의 처리가 존재 x

        if (emoticon.getCreator() != null) {
            notifyCreator(emoticon, EmoticonStatus.REJECT);
        }

    }

    private void notifyCreator(Emoticon emoticon, EmoticonStatus status) {
        EmoticonNotificationEvent event = EmoticonNotificationEvent.builder()
                .recipientId(emoticon.getCreator().getId())
                .emoticonId(emoticon.getId())
                .emoticonName(emoticon.getName())
                .status(status)
                .build();
        notificationService.processNotification(event);
    }

    @Transactional
    public List<EmoticonAdminResponse> uploadBulkEmoticons(Long userId, MultipartFile[] files, BulkEmoticonUploadRequest request) throws IOException {
        List<Emoticon> savedEmoticons = new ArrayList<>();
        User creator = userService.findUserById(userId);

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            EmoticonUploadRequest item = request.getEmoticons().get(i);

            fileStorageService.validateFile(file);
            FileInfo fileInfo = fileStorageService.storeFile(file, emoticonDir);

            Emoticon emoticon = Emoticon.builder()
                    .name(item.getName())
                    .storedName(fileInfo.getStoredFileName())
                    .imageUrl(emoticonUrlPrefix + fileInfo.getStoredFileName())
                    .contentType(fileInfo.getContentType())
                    .fileSize(fileInfo.getFileSize())
                    .price(item.getPrice())
                    .isDefault(request.isDefault())
                    .creator(creator)
                    .build();

            emoticon.updateStatus(EmoticonStatus.ACCEPT);
            emoticon = emoticonRepository.save(emoticon);

            UserEmoticon userEmoticon = UserEmoticon.builder()
                    .user(creator)
                    .emoticon(emoticon)
                    .type(EmoticonType.CREATED)
                    .purchasePrice(0)
                    .build();

            userEmoticonRepository.save(userEmoticon);
            savedEmoticons.add(emoticon);
        }

        return savedEmoticons.stream()
                .map(emoticon -> EmoticonAdminResponse.builder()
                        .id(emoticon.getId())
                        .name(emoticon.getName())
                        .imageUrl(emoticon.getImageUrl())
                        .price(emoticon.getPrice())
                        .contentType(emoticon.getContentType())
                        .fileSize(emoticon.getFileSize())
                        .createdAt(emoticon.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
