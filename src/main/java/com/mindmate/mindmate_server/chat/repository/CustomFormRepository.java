package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.CustomForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFormRepository extends JpaRepository<CustomForm, Long> {
    List<CustomForm> findByChatRoomId(Long chatRoomId);
}
