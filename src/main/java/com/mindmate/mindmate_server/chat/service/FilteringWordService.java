package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.FilteringWord;
import com.mindmate.mindmate_server.chat.dto.FilteringWordDTO;

import java.util.List;

public interface FilteringWordService {
    List<FilteringWordDTO> getAllFilteringWords();

    FilteringWordDTO addFilteringWord(String word);

    void deleteFilteringWord(Long id);

    FilteringWordDTO setFilteringWordActive(Long id, boolean active);

    FilteringWord findFilteringWordById(Long id);
}
