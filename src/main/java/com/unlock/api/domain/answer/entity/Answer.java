package com.unlock.api.domain.answer.entity;

import com.unlock.api.domain.common.BaseTimeEntity;
import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 질문에 대한 사용자 답변 엔티티
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 답변 내용

    @Builder.Default
    @Column(nullable = false)
    private boolean isRevealed = false; // 파트너의 답변을 보기 위해 광고 시청/결제를 완료했는지 여부
}
