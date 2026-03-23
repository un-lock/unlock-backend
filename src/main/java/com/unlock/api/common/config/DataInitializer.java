package com.unlock.api.common.config;

import com.unlock.api.domain.question.entity.Question;
import com.unlock.api.domain.question.entity.QuestionCategory;
import com.unlock.api.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 애플리케이션 기동 시 초기 데이터를 설정하는 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        // 질문 데이터가 없을 경우에만 초기 데이터 삽입
        if (questionRepository.count() == 0) {
            log.debug("초기 질문 데이터 생성을 시작합니다...");
            
            List<Question> initialQuestions = Arrays.asList(
                createQuestion("상대방의 신체 부위 중 가장 매력적이라고 느끼는 곳은 어디야?", QuestionCategory.SPICY),
                createQuestion("우리가 나눈 스킨십 중 가장 기억에 남는 순간은 언제야?", QuestionCategory.SPICY),
                createQuestion("서로의 은밀한 취향 중 하나를 더 알게 된다면 어떤 걸 알고 싶어?", QuestionCategory.SPICY)
            );

            questionRepository.saveAll(initialQuestions);
            log.debug("{}개의 초기 질문 데이터가 생성되었습니다.", initialQuestions.size());
        }
    }

    private Question createQuestion(String content, QuestionCategory category) {
        return Question.builder()
                .content(content)
                .category(category)
                .build();
    }
}