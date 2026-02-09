package com.unlock.api.domain.question.entity;

import com.unlock.api.domain.common.BaseTimeEntity;
import com.unlock.api.domain.couple.entity.Couple;
import jakarta.persistence.Column;
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

import java.time.LocalDate;

/**
 * 커플별 배정된 질문 기록 엔티티
 * 각 커플에게 날짜별로 어떤 질문이 랜덤하게 배정되었는지 관리합니다.
 */
@Entity
@Table(name = "couple_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoupleQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    private Couple couple; // 질문을 배정받은 커플

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // 배정된 질문

    @Column(nullable = false)
    private LocalDate assignedDate; // 질문이 배정된 날짜

    /**
     * 배정 날짜 업데이트 (미완료 질문을 오늘 날짜로 이동시킬 때 사용)
     */
    public void updateAssignedDate(LocalDate newDate) {
        this.assignedDate = newDate;
    }
}
