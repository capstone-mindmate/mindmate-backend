package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.review.domain.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
}
