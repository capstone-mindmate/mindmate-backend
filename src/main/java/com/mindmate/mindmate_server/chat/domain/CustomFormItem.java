package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "custom_form_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomFormItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_form_id")
    private CustomForm customForm;

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Builder
    public CustomFormItem(CustomForm customForm, String question) {
        this.customForm = customForm;
        this.question = question;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
