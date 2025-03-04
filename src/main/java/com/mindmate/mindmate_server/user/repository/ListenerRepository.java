package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ListenerRepository extends JpaRepository<ListenerProfile, Long> {
    boolean existsByNickname(String nickname);

    List<ListenerProfile> findByCertificationUrlIsNotNull();

    List<ListenerProfile> findByCounselingStyle(CounselingStyle counselingStyle);

    // 특정 상담 분야
    @Query("SELECT DISTINCT lp FROM ListenerProfile lp JOIN lp.counselingFields cf " +
            "WHERE cf.field = :field")
    List<ListenerProfile> findByField(@Param("field") CounselingField field);

    // 여러 상담 분야 중 하나
    @Query("SELECT DISTINCT lp FROM ListenerProfile lp JOIN lp.counselingFields cf " +
            "WHERE cf.field IN :fields")
    List<ListenerProfile> findByFields(@Param("fields") Set<CounselingField> fields);

    // 특정 상담 분야 + 스타일
    @Query("SELECT DISTINCT lp FROM ListenerProfile lp JOIN lp.counselingFields cf " +
            "WHERE cf.field IN :fields AND lp.counselingStyle = :style")
    List<ListenerProfile> findByFieldsAndStyle(
            @Param("fields") Set<CounselingField> fields,
            @Param("style") CounselingStyle style);
}
