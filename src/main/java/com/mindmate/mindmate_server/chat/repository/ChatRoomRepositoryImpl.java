package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.domain.QChatMessage;
import com.mindmate.mindmate_server.chat.domain.QChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.QMatching;
import com.mindmate.mindmate_server.user.domain.QProfile;
import com.mindmate.mindmate_server.user.domain.QProfileImage;
import com.mindmate.mindmate_server.user.domain.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {
    @Value("${profile.dir}")
    private String profileImageDir;

    @Value("${profile.url.prefix}")
    private String profileImageUrlPrefix;

    @Value("${profile.default.filename}")
    private String defaultProfileImageFilename;

    private final JPAQueryFactory queryFactory;

    // todo: admin용 채팅방 목록 조회는 없음 -> 지금 자신의 채팅방 목록 확인만 존재

    @Override
    public Page<ChatRoomResponse> findAllByUserId(Long userId, Pageable pageable) {
        return fetchChatRooms(userId, pageable, null, null);
    }

    @Override
    public Page<ChatRoomResponse> findAllByUserIdAndRole(Long userId, String roleType, Pageable pageable) {
        BooleanExpression roleCondition = createRoleCondition(userId, roleType);
        return fetchChatRooms(userId, pageable, roleCondition, null);
    }

    @Override
    public Page<ChatRoomResponse> findAllByUserIdAndStatus(Long userId, ChatRoomStatus status, Pageable pageable) {
        BooleanExpression statusCondition = QChatRoom.chatRoom.chatRoomStatus.eq(status);
        return fetchChatRooms(userId, pageable, null, statusCondition);
    }

    private Page<ChatRoomResponse> fetchChatRooms(Long userId, Pageable pageable,
                                                  BooleanExpression roleCondition,
                                                  BooleanExpression statusCondition) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QMatching matching = QMatching.matching;
        QUser creator = new QUser("creator");
        QUser acceptedUser = new QUser("acceptedUser");
        QChatMessage lastMessage = new QChatMessage("lastMessage");
        QProfile creatorProfile = new QProfile("creatorProfile");
        QProfile acceptedUserProfile = new QProfile("acceptedUserProfile");
        QProfileImage creatorProfileImage = new QProfileImage("creatorProfileImage");
        QProfileImage acceptedUserProfileImage = new QProfileImage("acceptedUserProfileImage");

        // 각 채팅방의 마지막 메시지 ID 조회
        JPQLQuery<Long> subQuery = JPAExpressions
                .select(lastMessage.id.max())
                .from(lastMessage)
                .where(lastMessage.chatRoom.eq(chatRoom));

        // 기본 조건: 사용자가 참여한 채팅방 + 삭제 여부 확인
        BooleanExpression baseCondition = (matching.creator.id.eq(userId)
                .and(chatRoom.deletedByListener.eq(false).or(chatRoom.deletedBySpeaker.eq(false).and(matching.creatorRole.eq(InitiatorType.SPEAKER)))))
                .or(matching.acceptedUser.id.eq(userId)
                        .and(chatRoom.deletedBySpeaker.eq(false).or(chatRoom.deletedByListener.eq(false).and(matching.creatorRole.eq(InitiatorType.LISTENER)))));

        // 추가 조건 적용
        if (roleCondition != null) {
            baseCondition = baseCondition.and(roleCondition);
        }

        if (statusCondition != null) {
            baseCondition = baseCondition.and(statusCondition);
        }

        List<Tuple> results = queryFactory
                .select(createSelectProjection(chatRoom, matching, lastMessage, userId,
                        creator, acceptedUser, creatorProfile, acceptedUserProfile,
                        creatorProfileImage, acceptedUserProfileImage))
                .from(chatRoom)
                .join(chatRoom.matching, matching)
                .join(matching.creator, creator)
                .join(creator.profile, creatorProfile)
                .leftJoin(creatorProfile.profileImage, creatorProfileImage)
                .leftJoin(matching.acceptedUser, acceptedUser)
                .leftJoin(acceptedUser.profile, acceptedUserProfile)
                .leftJoin(acceptedUserProfile.profileImage, acceptedUserProfileImage)
                .leftJoin(lastMessage).on(lastMessage.id.eq(subQuery))
                .where(baseCondition)
                .orderBy(chatRoom.lastMessageTime.desc().nullsLast())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // DTO 변환
        List<ChatRoomResponse> content = convertToDto(results, chatRoom, lastMessage);

        // 전체 개수 조회
        long total = queryFactory
                .selectFrom(chatRoom)
                .join(chatRoom.matching, matching)
                .where(baseCondition)
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }


    private Expression<?>[] createSelectProjection(QChatRoom chatRoom, QMatching matching,
                                                   QChatMessage lastMessage, Long userId,
                                                   QUser creator, QUser acceptedUser,
                                                   QProfile creatorProfile, QProfile acceptedUserProfile,
                                                   QProfileImage creatorProfileImage,
                                                   QProfileImage acceptedUserProfileImage) {
//        QProfile creatorProfile = new QProfile("creatorProfile");
//        QProfile acceptedUserProfile = new QProfile("acceptedUserProfile");
//        QUser creator = new QUser("creator");
//        QUser acceptedUser = new QUser("acceptedUser");

        String imageUrl = profileImageUrlPrefix + defaultProfileImageFilename;

        return new Expression<?>[]{
                chatRoom.id,
                chatRoom.matching.id,
                chatRoom.chatRoomStatus,
                chatRoom.lastMessageTime,
                lastMessage.content,
                lastMessage.type,
                matching.creator.id.eq(userId).as("isCreator"),
                matching.title.as("matchingTitle"),
                // 상대방 이름
                new CaseBuilder()
                        .when(matching.creator.id.eq(userId).and(matching.acceptedUser.isNotNull()))
                        .then(acceptedUserProfile.nickname)
                        .when(matching.creator.id.eq(userId).and(matching.acceptedUser.isNull()))
                        .then("대기 중")
                        .otherwise(creatorProfile.nickname).as("oppositeName"),
                // 상대방 이미지
                new CaseBuilder()
                        .when(matching.creator.id.eq(userId).and(matching.acceptedUser.isNotNull()))
                        .then(acceptedUserProfileImage.imageUrl)
                        .when(matching.creator.id.eq(userId).and(matching.acceptedUser.isNull()))
                        .then(imageUrl)
                        .otherwise(creatorProfileImage.imageUrl).as("oppositeImage"),
                // 상대방 ID - 타입 모호성 해결
                new CaseBuilder()
                        .when(matching.creator.id.eq(userId).and(matching.acceptedUser.isNotNull()))
                        .then(acceptedUser.id)
                        .otherwise(creator.id).as("oppositeId"),
                // 안읽은 메시지 수
                new CaseBuilder()
                        .when(
                                // 사용자가 생성자이면서 리스너 역할
                                matching.creator.id.eq(userId).and(matching.creatorRole.eq(InitiatorType.LISTENER))
                                        .or(
                                                // 사용자가 수락자이면서 생성자가 스피커 역할 (즉, 수락자가 리스너)
                                                matching.acceptedUser.id.eq(userId).and(matching.creatorRole.eq(InitiatorType.SPEAKER))
                                        )
                        )
                        .then(chatRoom.listenerUnreadCount)
                        .otherwise(chatRoom.speakerUnreadCount).as("unreadCount"),
//                new CaseBuilder()
//                        // 사용자가 리스너인 경우
//                        .when(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId))
//                                .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId))))
//                        .then(chatRoom.listenerUnreadCount)
//                        // 사용자가 스피커인 경우
//                        .otherwise(chatRoom.speakerUnreadCount).as("unreadCount"),
                // 사용자 역할
                new CaseBuilder()
                        .when(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId))
                                .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId))))
                        .then("LISTENER")
                        .otherwise("SPEAKER").as("userRole"),
                matching.category.as("category")
        };
    }



    private BooleanExpression createRoleCondition(Long userId, String roleType) {
        QMatching matching = QMatching.matching;

        if ("LISTENER".equals(roleType)) {
            return (matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId)))
                    .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId)));
        } else { // "SPEAKER"
            return (matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.creator.id.eq(userId)))
                    .or(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.acceptedUser.id.eq(userId)));
        }
    }

    private List<ChatRoomResponse> convertToDto(List<Tuple> results, QChatRoom chatRoom, QChatMessage lastMessage) {
        return results.stream()
                .map(tuple -> {
                    String lastMessageContent = "새 채팅방이 생성되었습니다";
                    MessageType lastMessageType = tuple.get(lastMessage.type);

                    if (lastMessageType != null) {
                        if (lastMessageType == MessageType.TEXT) {
                            lastMessageContent = "💬 " + "암호화된 메시지";
                        } else if (lastMessageType == MessageType.CUSTOM_FORM) {
                            lastMessageContent = "📝 " + "커스텀 폼";
                        } else if (lastMessageType == MessageType.EMOTICON) {
                            lastMessageContent = "😊 " + "이모티콘";
                        }
                    }

                    return ChatRoomResponse.builder()
                            .roomId(tuple.get(chatRoom.id))
                            .matchingId(tuple.get(chatRoom.matching.id))
                            .chatRoomStatus(tuple.get(chatRoom.chatRoomStatus))
                            .lastMessageTime(tuple.get(chatRoom.lastMessageTime))
                            .lastMessage(lastMessageContent)
                            .isCreator(Boolean.TRUE.equals(tuple.get(6, Boolean.class)))
                            .matchingTitle(tuple.get(7, String.class))
                            .oppositeName(tuple.get(8, String.class))
                            .oppositeImage(tuple.get(9, String.class))
                            .oppositeId(tuple.get(10, Long.class))
                            .unreadCount(tuple.get(11, Long.class) != null ? tuple.get(11, Long.class) : 0L)
                            .userRole(tuple.get(12, String.class))
                            .category(tuple.get(13, MatchingCategory.class))
                            .build();
                })
                .collect(Collectors.toList());
    }
}