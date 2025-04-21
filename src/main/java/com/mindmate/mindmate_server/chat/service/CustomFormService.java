package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.CustomForm;
import com.mindmate.mindmate_server.chat.dto.CustomFormRequest;
import com.mindmate.mindmate_server.chat.dto.CustomFormResponse;
import com.mindmate.mindmate_server.chat.dto.RespondToCustomFormRequest;

import java.util.List;

public interface CustomFormService {
    CustomFormResponse createCustomForm(Long userId, CustomFormRequest request);

    CustomForm findCustomFormById(Long formId);

    CustomFormResponse respondToCustomForm(Long formId, Long userId, RespondToCustomFormRequest request);

    List<CustomFormResponse> getCustomFormsByChatRoom(Long chatRoomId);
}
