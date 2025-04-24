package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonDetailResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.dto.UserEmoticonResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface EmoticonService {
    List<EmoticonResponse> getShopEmoticons(Long userId);

    EmoticonDetailResponse getEmoticonDetail(Long emoticonId, Long userId);

    UserEmoticonResponse getUserEmoticons(Long userId);

    List<EmoticonResponse> getAvailableEmoticons(Long userId);

    Emoticon findEmoticonById(Long emoticonId);


    EmoticonResponse uploadEmoticon(MultipartFile file, EmoticonUploadRequest request, Long userId) throws IOException;

    EmoticonResponse purchaseEmoticon(Long userId, Long emoticonId);
}
