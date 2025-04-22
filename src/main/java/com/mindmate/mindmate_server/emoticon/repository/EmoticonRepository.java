package com.mindmate.mindmate_server.emoticon.repository;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmoticonRepository extends JpaRepository<Emoticon, Long> {
    List<Emoticon> findByStatusOrderByCreatedAtDesc(EmoticonStatus status);

    List<Emoticon> findByStatusAndIsDefaultOrderByCreatedAtDesc(EmoticonStatus status, boolean isDefault);

}
