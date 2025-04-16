package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToastBoxRepository extends JpaRepository<ToastBoxKeyword, Long> {
    Optional<ToastBoxKeyword> findByKeyword(String keyword);
    List<ToastBoxKeyword> findByActiveTrue();
    List<ToastBoxKeyword> findByKeywordInAndActiveTrue(Collection<String> keywords);
}
