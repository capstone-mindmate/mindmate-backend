package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.Matching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {
}
