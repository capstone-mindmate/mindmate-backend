package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.QChatMessage;
import com.mindmate.mindmate_server.chat.domain.QChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.QMatching;
import com.mindmate.mindmate_server.user.domain.QProfile;
import com.mindmate.mindmate_server.user.domain.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    
    // todo: admin용 채팅방 목록 조회는 없음 -> 지금 자신의 채팅방 목록 확인만 존재

    @Override
    public Page<ChatRoomResponse> findAllByUserId(Long userId, Pageable pageable) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QMatching matching = QMatching.matching;
        QUser creator = new QUser("creator");
        QUser acceptedUser = new QUser("acceptedUser");
        QChatMessage lastMessage = new QChatMessage("lastMessage");
        QProfile creatorProfile = new QProfile("creatorProfile");
        QProfile acceptedUserProfile = new QProfile("acceptedUserProfile");

        // 각 채팅방의 마지막 메시지 ID 조회
        JPQLQuery<Long> subQuery = JPAExpressions
                .select(lastMessage.id.max())
                .from(lastMessage)
                .where(lastMessage.chatRoom.eq(chatRoom));

        List<Tuple> results = queryFactory
                .select(
                        chatRoom.id,
                        chatRoom.chatRoomStatus,
                        chatRoom.lastMessageTime,
                        lastMessage.content,
                        // 상대방 이름
                        new CaseBuilder()
                                .when(matching.creator.id.eq(userId))
                                .then(acceptedUserProfile.nickname)
                                .otherwise(creatorProfile.nickname).as("oppositeName"),
                        // 상대방 이미지
                        new CaseBuilder()
                                .when(matching.creator.id.eq(userId))
                                .then(acceptedUserProfile.profileImage)
                                .otherwise(creatorProfile.profileImage).as("oppositeImage"),
                        // 안읽은 메시지 수
                        new CaseBuilder()
                                // 사용자가 리스너인 경우
                                .when(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId))
                                        .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId))))
                                .then(chatRoom.listenerUnreadCount)
                                // 사용자가 스피커인 경우
                                .otherwise(chatRoom.speakerUnreadCount).as("unreadCount"),
                        // 사용자 역할
                        new CaseBuilder()
                                .when(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId))
                                        .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId))))
                                .then("LISTENER")
                                .otherwise("SPEAKER").as("userRole")
                )
                .from(chatRoom)
                .join(chatRoom.matching, matching)
                .join(matching.creator, creator)
                .join(creator.profile, creatorProfile)
                .leftJoin(matching.acceptedUser, acceptedUser)
                .leftJoin(acceptedUser.profile, acceptedUserProfile)
                .leftJoin(lastMessage).on(lastMessage.id.eq(subQuery))
                .where(matching.creator.id.eq(userId).or(matching.acceptedUser.id.eq(userId)))
                .orderBy(chatRoom.lastMessageTime.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // DTO 변환
        List<ChatRoomResponse> content = results.stream()
                .map(tuple -> ChatRoomResponse.builder()
                        .roomId(tuple.get(chatRoom.id))
                        .chatRoomStatus(tuple.get(chatRoom.chatRoomStatus))
                        .lastMessageTime(tuple.get(chatRoom.lastMessageTime))
                        .lastMessage(tuple.get(lastMessage.content))
                        .oppositeName(tuple.get(4, String.class)) // "oppositeName" alias
                        .oppositeImage(tuple.get(5, String.class)) // "oppositeImage" alias
                        .unreadCount(tuple.get(6, Integer.class)) // "unreadCount" alias
                        .userRole(tuple.get(7, String.class))
                        .build())
                .collect(Collectors.toList());

        long total = queryFactory
                .selectFrom(chatRoom)
                .join(chatRoom.matching, matching)
                .where(matching.creator.id.eq(userId).or(matching.acceptedUser.id.eq(userId)))
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<ChatRoomResponse> findAllByUserIdAndRole(Long userId, String roleType, Pageable pageable) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QMatching matching = QMatching.matching;
        QUser creator = new QUser("creator");
        QUser acceptedUser = new QUser("acceptedUser");
        QChatMessage lastMessage = new QChatMessage("lastMessage");
        QProfile creatorProfile = new QProfile("creatorProfile");
        QProfile acceptedUserProfile = new QProfile("acceptedUserProfile");

        // 각 채팅방의 마지막 메시지 ID 조회
        JPQLQuery<Long> subQuery = JPAExpressions
                .select(lastMessage.id.max())
                .from(lastMessage)
                .where(lastMessage.chatRoom.eq(chatRoom));

        BooleanExpression roleCondition;
        if ("LISTENER".equals(roleType)) {
            roleCondition = (matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId)))
                    .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId)));
        } else { // "SPEAKER"
            roleCondition = (matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.creator.id.eq(userId)))
                    .or(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.acceptedUser.id.eq(userId)));
        }

        List<Tuple> results = queryFactory
                .select(
                        chatRoom.id,
                        chatRoom.chatRoomStatus,
                        chatRoom.lastMessageTime,
                        lastMessage.content,
                        new CaseBuilder()
                                .when(matching.creator.id.eq(userId))
                                .then(acceptedUserProfile.nickname)
                                .otherwise(creatorProfile.nickname).as("oppositeName"),
                        // 상대방 이미지
                        new CaseBuilder()
                                .when(matching.creator.id.eq(userId))
                                .then(acceptedUserProfile.profileImage)
                                .otherwise(creatorProfile.profileImage).as("oppositeImage"),
                        // 안읽은 메시지 수
                        new CaseBuilder()
                                // 사용자가 리스너인 경우
                                .when(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId))
                                        .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId))))
                                .then(chatRoom.listenerUnreadCount)
                                // 사용자가 스피커인 경우
                                .otherwise(chatRoom.speakerUnreadCount).as("unreadCount"),
                        // 사용자 역할
                        new CaseBuilder()
                                .when(matching.creatorRole.eq(InitiatorType.LISTENER).and(matching.creator.id.eq(userId))
                                        .or(matching.creatorRole.eq(InitiatorType.SPEAKER).and(matching.acceptedUser.id.eq(userId))))
                                .then("LISTENER")
                                .otherwise("SPEAKER").as("userRole")
                )
                .from(chatRoom)
                .join(chatRoom.matching, matching)
//                .leftJoin(chatRoom.matching, matching)  // inner join 대신 left join 사용
                .join(matching.creator, creator)
                .join(creator.profile, creatorProfile)
                .leftJoin(matching.acceptedUser, acceptedUser)
                .leftJoin(acceptedUser.profile, acceptedUserProfile)
                .leftJoin(lastMessage).on(lastMessage.id.eq(subQuery))
                .where(roleCondition)
                .orderBy(chatRoom.lastMessageTime.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // DTO 변환
        List<ChatRoomResponse> content = results.stream()
                .map(tuple -> ChatRoomResponse.builder()
                        .roomId(tuple.get(chatRoom.id))
                        .chatRoomStatus(tuple.get(chatRoom.chatRoomStatus))
                        .lastMessageTime(tuple.get(chatRoom.lastMessageTime))
                        .lastMessage(tuple.get(lastMessage.content))
                        .oppositeName(tuple.get(4, String.class)) // "oppositeName" alias
                        .oppositeImage(tuple.get(5, String.class)) // "oppositeImage" alias
                        .unreadCount(tuple.get(6, Integer.class)) // "unreadCount" alias
                        .userRole(tuple.get(7, String.class))
                        .build())
                .collect(Collectors.toList());

        long total = queryFactory
                .selectFrom(chatRoom)
                .join(chatRoom.matching, matching)
                .where(roleCondition)
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }
}
