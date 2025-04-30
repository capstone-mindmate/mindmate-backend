package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface EmoticonService {
    List<EmoticonResponse> getShopEmoticons(Long userId);

    EmoticonDetailResponse getEmoticonDetail(Long emoticonId, Long userId);

    UserEmoticonResponse getUserEmoticons(Long userId);

    Set<Long> getOwnedEmoticonIds(Long userId);

    List<EmoticonResponse> getAvailableEmoticons(Long userId);

    Emoticon findEmoticonById(Long emoticonId);


    EmoticonResponse uploadEmoticon(MultipartFile file, EmoticonUploadRequest request, Long userId) throws IOException;

    EmoticonResponse purchaseEmoticon(Long userId, Long emoticonId);

    boolean isEmoticonOwnedByUser(Long userId, Long emoticonId);

    EmoticonMessageResponse sendEmoticonMessage(Long userId, EmoticonMessageRequest request);
}
