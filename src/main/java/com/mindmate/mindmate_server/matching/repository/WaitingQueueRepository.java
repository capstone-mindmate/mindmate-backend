package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.WaitingQueue;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {

    // todo : querydsl 사용

    // 요청자 타입 & 활성 여부
    List<WaitingQueue> findByWaitingTypeAndActiveOrderByCreatedAtAsc(InitiatorType waitingType, boolean active);

    // 타입, 활성 여부, 스타일
    List<WaitingQueue> findByWaitingTypeAndActiveAndPreferredStyleOrderByCreatedAtAsc(
            InitiatorType waitingType, boolean active, CounselingStyle preferredStyle);

    // 특정 유저 활성 여부
    Optional<WaitingQueue> findBySpeakerProfileIdAndActiveTrue(Long speakerProfileId);
    Optional<WaitingQueue> findByListenerProfileIdAndActiveTrue(Long listenerProfileId);

    // 특정 상담 분야
    @Query("SELECT wq FROM WaitingQueue wq WHERE wq.waitingType = 'SPEAKER' AND wq.active = true " +
            "AND :field MEMBER OF wq.preferredFields ORDER BY wq.createdAt ASC")
    List<WaitingQueue> findActiveSpeakersByPreferredField(@Param("field") CounselingField field);

    @Query("SELECT wq FROM WaitingQueue wq JOIN wq.listenerProfile lp JOIN lp.counselingFields cf " +
            "WHERE wq.waitingType = 'LISTENER' AND wq.active = true " +
            "AND cf.field = :field ORDER BY wq.createdAt ASC")
    List<WaitingQueue> findActiveListenersByField(@Param("field") CounselingField field);

    // 여러 상담 분야 중 하나
    @Query("SELECT wq FROM WaitingQueue wq WHERE wq.waitingType = 'SPEAKER' AND wq.active = true " +
            "AND wq.preferredFields IN :fields ORDER BY wq.createdAt ASC")
    List<WaitingQueue> findActiveSpeakersByPreferredFields(@Param("fields") Set<CounselingField> fields);

    @Query("SELECT DISTINCT wq FROM WaitingQueue wq JOIN wq.listenerProfile lp JOIN lp.counselingFields cf " +
            "WHERE wq.waitingType = 'LISTENER' AND wq.active = true " +
            "AND cf.field IN :fields ORDER BY wq.createdAt ASC")
    List<WaitingQueue> findActiveListenersByPreferredFields(@Param("fields") Set<CounselingField> fields);


    // 상담 분야 + 스타일
    @Query("SELECT wq FROM WaitingQueue wq WHERE wq.waitingType = 'SPEAKER' AND wq.active = true " +
            "AND wq.preferredFields IN :fields AND wq.preferredStyle = :style ORDER BY wq.createdAt ASC")
    List<WaitingQueue> findActiveSpeakersByFieldsAndStyle(
            @Param("fields") Set<CounselingField> fields,
            @Param("style") CounselingStyle style);

    @Query("SELECT DISTINCT wq FROM WaitingQueue wq JOIN wq.listenerProfile lp JOIN lp.counselingFields cf " +
            "WHERE wq.waitingType = 'LISTENER' AND wq.active = true " +
            "AND cf.field IN :fields AND lp.counselingStyle = :style ORDER BY wq.createdAt ASC")
    List<WaitingQueue> findActiveListenersByFieldsAndStyle(
            @Param("fields") Set<CounselingField> fields,
            @Param("style") CounselingStyle style);
}
