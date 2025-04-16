package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToastBoxRepository extends JpaRepository<ToastBoxKeyword, Long> {
    List<ToastBoxKeyword> findByActiveTrue();

}
