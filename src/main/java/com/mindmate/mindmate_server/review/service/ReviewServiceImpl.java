package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.*;
import com.mindmate.mindmate_server.notification.dto.ReviewCreatedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.domain.Tag;
import com.mindmate.mindmate_server.review.domain.TagType;
import com.mindmate.mindmate_server.review.dto.*;
import com.mindmate.mindmate_server.review.repository.ReviewRedisRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.ProfileService;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final ReviewRedisRepository reviewRedisRepository;

    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final ProfileService profileService;
    private final NotificationService notificationService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        validateRating(request.getRating());

        User reviewer = userService.findUserById(userId);
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getChatRoomId());

        validateChatRoomStatus(chatRoom);

        User reviewedUser = getReviewedUser(chatRoom, reviewer);
        TagType tagType = getTagType(chatRoom, reviewer);

        checkValidateReview(chatRoom, reviewer, reviewedUser);

        Profile reviewedProfile = getReviewedProfile(reviewedUser);
        Review review = buildAndSaveReview(chatRoom, reviewer, reviewedProfile, request);

        addReviewTags(review, request.getTags(), tagType, reviewedProfile.getId());
        updateProfileMetrics(userId, request.getRating());
        reviewRedisRepository.deleteAllProfileCaches(reviewedProfile.getId());

        processReviewRewards(reviewer, reviewedUser, review.getId());

        return ReviewResponse.from(review);
    }

    private void addReviewTags(Review review, List<String> tags, TagType tagType, Long profileId) {
        if (tags != null && !tags.isEmpty()) {
            for (String tagContent : tags) {
                try {
                    Tag tag = Tag.fromContent(tagContent);

                    if (tag.getType() != tagType) {
                        throw new CustomException(ReviewErrorCode.INVALID_REVIEW_TAGS);
                    }
                    review.addTag(tag);

                    reviewRedisRepository.incrementTagCount(profileId, tagContent);
                } catch (IllegalArgumentException e) {
                    throw new CustomException(ReviewErrorCode.INVALID_REVIEW_TAGS);
                }
            }
        }
    }

    private Review buildAndSaveReview(ChatRoom chatRoom, User reviewer, Profile reviewedProfile, ReviewRequest request) {
        Review review = Review.builder()
                .chatRoom(chatRoom)
                .reviewer(reviewer)
                .reviewedProfile(reviewedProfile)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return reviewRepository.save(review);
    }

    private void checkValidateReview(ChatRoom chatRoom, User reviewer, User reviewedUser) {

        if (reviewedUser.getId().equals(reviewer.getId())) {
            throw new CustomException(ReviewErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }

        if (reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)) {
            throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    private TagType getTagType(ChatRoom chatRoom, User reviewer) {

        if (chatRoom.isSpeaker(reviewer)) {
            return TagType.LISTENER;
        } else {
            return TagType.SPEAKER;
        }

    }

    private User getReviewedUser(ChatRoom chatRoom, User reviewer) {

        if (chatRoom.isListener(reviewer)) {
            return chatRoom.getSpeaker();
        } else if (chatRoom.isSpeaker(reviewer)) {
            return chatRoom.getListener();
        } else {
            throw new CustomException(ChatErrorCode.USER_NOT_IN_CHAT);
        }
    }

    private void validateRating(int rating) {

        if (rating < 1 || rating > 5) {
            throw new CustomException(ReviewErrorCode.INVALID_RATING_VALUE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProfileReviews(Long profileId, int page, int size, String sortType){

        Profile profile = profileService.findProfileById(profileId);
        Page<Review> reviews = fetchSortedReviews(profile, page, size, sortType);

        return reviews.map(ReviewResponse::from);
    }

    private Page<Review> fetchSortedReviews(Profile profile, int page, int size, String sortType) {
        Pageable pageable;
        switch (sortType) {
            case "highest_rating":
                pageable = PageRequest.of(page, size);
                return reviewRepository.findByReviewedProfileOrderByRatingDesc(profile, pageable);
            case "lowest_rating":
                pageable = PageRequest.of(page, size);
                return reviewRepository.findByReviewedProfileOrderByRatingAsc(profile, pageable);
            case "latest":
            default:
                pageable = PageRequest.of(page, size);
                return reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(profile, pageable);
        }
    }

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

        ChatRoom chatRoom = chatRoomService.findChatRoomById(chatRoomId);

        return reviewRepository.findByChatRoom(chatRoom).stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReview(Long userId, Long chatRoomId) {

        User user = userService.findUserById(userId);
        ChatRoom chatRoom = chatRoomService.findChatRoomById(chatRoomId);

        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.CLOSED) {
            return false;
        }
        if (!chatRoom.isListener(user) && !chatRoom.isSpeaker(user)) {
            return false;
        }

        return !reviewRepository.existsByChatRoomAndReviewer(chatRoom, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileReviewSummaryResponse getProfileReviewSummary(Long profileId) {

        ProfileReviewSummaryResponse cachedSummary = reviewRedisRepository.getReviewSummary(profileId);
        if (cachedSummary != null) {
            return cachedSummary;
        }

        Profile profile = profileService.findProfileById(profileId);

        double avgRating = profile.getAvgRating();
        long totalReviews = reviewRepository.countByReviewedProfile(profile);

        Map<String, Integer> tagCounts = getTagCounts(profileId, profile);

        List<ReviewListResponse> recentReviewResponses = getRecentReviews(profile);

        return buildSummaryResponse(
                avgRating, totalReviews, tagCounts, recentReviewResponses, profileId);
    }

    private List<ReviewListResponse> getRecentReviews(Profile profile) {

        Page<Review> reviewsPage = reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(
                profile,
                PageRequest.of(0, 5)
        );

        return reviewsPage.getContent().stream()
                .map(ReviewListResponse::from)
                .collect(Collectors.toList());

    }

    private Map<String, Integer> getTagCounts(Long profileId, Profile profile) {

        Map<String, Integer> cachedTagCounts = reviewRedisRepository.getTagCounts(profileId);

        if (cachedTagCounts != null && !cachedTagCounts.isEmpty()) {
            return cachedTagCounts;
        } else {
            List<Object[]> tagCountResults = reviewRepository.countAllTagsByProfile(profile);
            Map<String, Integer> tagCounts = new HashMap<>();

            for (Object[] result : tagCountResults) {
                String tagContent = (String) result[0];
                Integer count = ((Long) result[1]).intValue();
                tagCounts.put(tagContent, count);
            }

            reviewRedisRepository.saveTagCounts(profileId, tagCounts);
            return tagCounts;
        }
    }

    private void validateChatRoomStatus(ChatRoom chatRoom) {
        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.CLOSED) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_NOT_CLOSED);
        }
    }

    private Profile getReviewedProfile(User reviewedUser) {
        Profile reviewedProfile = reviewedUser.getProfile();
        if (reviewedProfile == null) {
            throw new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND);
        }
        return reviewedProfile;
    }

    private void processReviewRewards(User reviewer, User reviewedUser, Long reviewId) {
        String reviewerName = reviewer.getProfile() != null ? reviewer.getProfile().getNickname() : "사용자";
        ReviewCreatedNotificationEvent event = ReviewCreatedNotificationEvent.builder()
                .recipientId(reviewedUser.getId())
                .reviewId(reviewId)
                .reviewerName(reviewerName)
                .build();

        notificationService.processNotification(event);
    }

    private ProfileReviewSummaryResponse buildSummaryResponse(
            double avgRating, long totalReviews, Map<String, Integer> tagCounts,
            List<ReviewListResponse> recentReviews, Long profileId) {

        ProfileReviewSummaryResponse summaryResponse = ProfileReviewSummaryResponse.builder()
                .averageRating(avgRating)
                .totalReviews((int) totalReviews)
                .tagCounts(tagCounts)
                .recentReviews(recentReviews)
                .build();

        reviewRedisRepository.saveReviewSummary(profileId, summaryResponse);
        return summaryResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getTagCountsByProfileId(Long profileId) {
        List<Object[]> tagCountResults = reviewRepository.countTagsByProfileId(profileId);
        Map<String, Integer> tagCounts = new HashMap<>();

        for (Object[] result : tagCountResults) {
            Tag tag = (Tag) result[0];
            Integer count = ((Long) result[1]).intValue();
            tagCounts.put(tag.getContent(), count);
        }

        return tagCounts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRecentReviewsByUserId(Long userId, int limit) {
        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(
                userId,
                PageRequest.of(0, limit)
        );

        return recentReviews.stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .tags(review.getReviewTags().stream()
                                .map(tag -> tag.getTagContent().getContent())
                                .collect(Collectors.toList()))
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingByUserId(Long userId) {
        return reviewRepository.calculateAverageRatingByRevieweeId(userId)
                .orElse(0.0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProfileMetrics(Long userId, double rating) {
        try {
            profileService.incrementCounselingCount(userId);
            profileService.updateAvgRating(userId, rating);
        } catch (OptimisticLockingFailureException e) {
            log.warn("프로필 업데이트 중 동시성 이슈 발생", e);
            throw new CustomException(ReviewErrorCode.REVIEW_SUBMISSION_CONFLICT);
        }

    }

    @Override
    public Review findReviewById(Long reviewId){
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }

}
