package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "filtering_words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilteringWord extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String word;

    private boolean active = true;

    @Builder
    public FilteringWord(String word) {
        this.word = word;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

}
