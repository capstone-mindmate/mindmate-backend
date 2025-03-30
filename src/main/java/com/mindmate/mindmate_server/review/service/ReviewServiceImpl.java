package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.*;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.domain.ReviewReply;
import com.mindmate.mindmate_server.review.domain.Tag;
import com.mindmate.mindmate_server.review.domain.TagType;
import com.mindmate.mindmate_server.review.dto.ReviewReplyRequest;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewReplyRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.repository.ProfileRepository;
import com.mindmate.mindmate_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ReviewReplyRepository reviewReplyRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new CustomException(ReviewErrorCode.INVALID_RATING_VALUE);
        }

        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.CLOSED) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_NOT_CLOSED);
        }

        // 리뷰 대상 결정ㅎ기
        User reviewedUser;
        if (chatRoom.isListener(reviewer)) {
            reviewedUser = chatRoom.getSpeaker();
        } else if (chatRoom.isSpeaker(reviewer)) {
            reviewedUser = chatRoom.getListener();
        } else {
            throw new CustomException(ChatErrorCode.USER_NOT_IN_CHAT);
        }

        // 자동으로 타입결정
        TagType tagType;
        if (chatRoom.isSpeaker(reviewer)) {
            tagType = TagType.LISTENER;
        } else {
            tagType = TagType.SPEAKER;
        }

        if (reviewedUser.getId().equals(reviewer.getId())) {
            throw new CustomException(ReviewErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }

        if (reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)) {
            throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Profile reviewedProfile = profileRepository.findByUser(reviewedUser)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        Review review = Review.builder()
                .chatRoom(chatRoom)
                .reviewer(reviewer)
                .reviewedProfile(reviewedProfile)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagContent : request.getTags()) {
                try {
                    Tag tag = Tag.fromContent(tagContent);

                    // 리스너는 스피커, 스피커는 리스너 평가
                    if (tag.getType() != tagType) {
                        throw new CustomException(ReviewErrorCode.INVALID_REVIEW_TAGS);
                    }
                    review.addTag(tag);
                } catch (IllegalArgumentException e) {
                    throw new CustomException(ReviewErrorCode.INVALID_REVIEW_TAGS);
                }
            }
        }

        // profile 업뎃
        reviewedProfile.incrementCounselingCount();
        reviewedProfile.updateAvgRating(request.getRating());

        return ReviewResponse.from(review);
    }

    @Override
    @Transactional
    public ReviewResponse createReviewReply(Long userId, ReviewReplyRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Review review = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND));

        Profile userProfile = profileRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        if (!review.getReviewedProfile().getId().equals(userProfile.getId())) {
            throw new CustomException(ReviewErrorCode.NOT_AUTHORIZED_TO_REPLY);
        }

        if (review.getReply() != null) {
            throw new CustomException(ReviewErrorCode.REPLY_ALREADY_EXISTS);
        }

        ReviewReply reply = ReviewReply.builder()
                .review(review)
                .content(request.getContent())
                .build();

        reviewReplyRepository.save(reply);
        review.setReply(reply);

        return ReviewResponse.from(review);
    }

   // todo : 리뷰 신고

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProfileReviews(Long profileId, int page, int size, String sortType){

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        Pageable pageable;
        Page<Review> reviews;

        switch (sortType) {
            case "highest_rating":
                pageable = PageRequest.of(page, size);
                reviews = reviewRepository.findByReviewedProfileOrderByRatingDesc(profile, pageable);
                break;
            case "lowest_rating":
                pageable = PageRequest.of(page, size);
                reviews = reviewRepository.findByReviewedProfileOrderByRatingAsc(profile, pageable);
                break;
            case "latest":
            default:
                pageable = PageRequest.of(page, size);
                reviews = reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(profile, pageable);
                break;
        }

        return reviews.map(ReviewResponse::from);
        // sortType -> 인기/최신?? (별점 높은거랑 낮은거?)
    } // 전부(리뷰+답글 다 포함) .. 흠 전체 세세하게 주기 vs 간략하게 주기
    // -> 전부 줄 필요가 있나?

    // 프로필에 보여줄 요약?? 평균별점+개수 + 태그

    // todo : 목록 보여주는 방식 결정하기.

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getChatRoomReviews(Long chatRoomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        List<Review> reviews = reviewRepository.findByChatRoom(chatRoom);
        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReview(Long userId, Long chatRoomId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.CLOSED) {
            return false;
        }

        if (!chatRoom.isListener(user) && !chatRoom.isSpeaker(user)) {
            return false;
        }

        return !reviewRepository.existsByChatRoomAndReviewer(chatRoom, user);
    }


}
