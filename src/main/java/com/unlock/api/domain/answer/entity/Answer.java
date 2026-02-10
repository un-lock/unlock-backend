package com.unlock.api.domain.answer.entity;

import com.unlock.api.common.security.util.AesEncryptionConverter;
import com.unlock.api.domain.common.BaseTimeEntity;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질문에 대한 사용자 답변 엔티티
 * AES-256 알고리즘을 사용하여 답변 내용을 암호화하여 저장합니다.
 */
@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Answer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // 답변 대상 질문

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 답변 작성자

    @Convert(converter = AesEncryptionConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 답변 내용 (AES-256 암호화 저장)
}