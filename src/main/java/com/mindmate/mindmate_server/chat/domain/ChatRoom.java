package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // todo : matching 매핑 + cascade 설정? 매칭이 사라지더라도 해당 데이터는 남길것인가
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id")
    private Matching matching;

    @Enumerated(EnumType.STRING)
    private ChatRoomStatus chatRoomStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listener_id")
    private ListenerProfile listener;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id")
    private SpeakerProfile speaker;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

//    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ChatRoomParticipant> participants = new ArrayList<>();

    private LocalDateTime lastMessageTime;
    private LocalDateTime closedAt;
//    private LocalDateTime expiryTime;

    // 읽음 상태 처리를 chatroomparticipant를 따로 두지 않고 바로 처리?
    private Long listenerLastReadMessageId = 0L;
    private Long speakerLastReadMessageId = 0L;
    private int listenerUnreadCount = 0;
    private int speakerUnreadCount = 0;

    @Builder
    public ChatRoom(Matching matching, ListenerProfile listener, SpeakerProfile speaker) {
        this.matching = matching;
        this.chatRoomStatus = ChatRoomStatus.ACTIVE;
        this.listener = listener;
        this.speaker = speaker;
//        this.expiryTime = expiryTime;
    }

    public void updateLastMessageTime() {
        this.lastMessageTime = LocalDateTime.now();
    }

    public void close() {
        this.chatRoomStatus = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void markAsReadForListener(Long messageId) {
        this.listenerLastReadMessageId = messageId;
        this.listenerUnreadCount = 0;
    }

    public void markAsReadForSpeaker(Long messageId) {
        this.speakerLastReadMessageId = messageId;
        this.speakerUnreadCount = 0;
    }

    public void increaseUnreadCountForListener() {
        this.listenerUnreadCount++;
    }

    public void increaseUnreadCountForSpeaker() {
        this.speakerUnreadCount++;
    }

}
