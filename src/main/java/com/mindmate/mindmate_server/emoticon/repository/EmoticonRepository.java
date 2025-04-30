package com.mindmate.mindmate_server.emoticon.repository;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmoticonRepository extends JpaRepository<Emoticon, Long> {
    List<Emoticon> findByStatusOrderByCreatedAtDesc(EmoticonStatus status);

    @Query("SELECT e FROM Emoticon e WHERE e.status = :status AND e.isDefault = :isDefault " +
            "AND e.id != :emoticonId AND ABS(e.price - :price) <= :priceRange " +
            "ORDER BY e.createdAt DESC")
    List<Emoticon> findSimilarPriceEmoticons(
            @Param("status") EmoticonStatus status,
            @Param("isDefault") boolean isDefault,
            @Param("emoticonId") Long emoticonId,
            @Param("price") int price,
            @Param("priceRange") int priceRange,
            Pageable pageable);
}
