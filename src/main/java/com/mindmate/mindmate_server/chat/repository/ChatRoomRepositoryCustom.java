package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatRoomRepositoryCustom {
    Page<ChatRoomResponse> findAllByUserId(Long userId, Pageable pageable);

    Page<ChatRoomResponse> findAllByUserIdAndRole(Long userId, String roleType, Pageable pageable);

    Page<ChatRoomResponse> findAllByUserIdAndStatus(Long userId, ChatRoomStatus status, Pageable pageable);
}
