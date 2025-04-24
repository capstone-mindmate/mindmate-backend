package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonAdminResponse;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminEmoticonService {
    private final EmoticonRepository emoticonRepository;
    private final EmoticonService emoticonService;

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

    // todo: 수락하면 해당 사용자에게 바로 귀속되도록
    @Transactional
    public void acceptEmoticon(Long emoticonId) {
        Emoticon emoticon = emoticonService.findEmoticonById(emoticonId);
        emoticon.updateStatus(EmoticonStatus.ACCEPT);
        emoticonRepository.save(emoticon);
    }

    @Transactional
    public void rejectEmoticon(Long emoticonId) {
        Emoticon emoticon = emoticonService.findEmoticonById(emoticonId);
        emoticon.updateStatus(EmoticonStatus.REJECT);
        emoticonRepository.save(emoticon);
    }
}
