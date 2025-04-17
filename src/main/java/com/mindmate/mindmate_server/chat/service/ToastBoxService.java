package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordDTO;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordRequest;

import java.util.List;

public interface ToastBoxService {

    void refreshToastBoxKeywords();

    List<ToastBoxKeyword> findToastBoxKeywords(String content);

    List<ToastBoxKeywordDTO> getAllToastBoxWords();

    ToastBoxKeywordDTO addToastBoxKeyword(ToastBoxKeywordRequest dto);

    ToastBoxKeywordDTO updateToastBoxKeyword(Long id, ToastBoxKeywordDTO dto);

    void deleteToastBoxKeyWord(Long id);

    ToastBoxKeywordDTO setToastBoxKeywordActive(Long id, boolean active);

    ToastBoxKeyword findToastBoxKeywordById(Long id);
}
