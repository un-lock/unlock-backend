package com.unlock.api.domain.question.entity;

import com.unlock.api.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 매일 제공되는 질문 엔티티
 */
@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Question extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 질문 내용

    @Column(nullable = false, unique = true)
    private LocalDate activeDate; // 질문이 공개되는 날짜
}
