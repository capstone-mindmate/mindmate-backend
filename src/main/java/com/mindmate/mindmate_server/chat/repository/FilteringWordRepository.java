package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilteringWordRepository extends JpaRepository<FilteringWord, Long> {
    List<FilteringWord> findByActiveTrue();

    Optional<FilteringWord> findByWord(String word);
}
