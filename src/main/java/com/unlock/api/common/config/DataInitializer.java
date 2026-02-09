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
            log.info("초기 질문 데이터 생성을 시작합니다...");
            
            List<Question> initialQuestions = Arrays.asList(
                createQuestion("오늘 하루 중 가장 나를 생각나게 했던 순간은 언제야?", QuestionCategory.DAILY),
                createQuestion("내가 입었을 때 가장 예쁘거나 멋있다고 생각하는 옷은 뭐야?", QuestionCategory.DAILY),
                createQuestion("우리가 처음 만났을 때, 나의 어떤 점에 가장 끌렸어?", QuestionCategory.ROMANCE),
                createQuestion("나랑 꼭 가보고 싶은 여행지나 해보고 싶은 데이트가 있어?", QuestionCategory.ROMANCE),
                createQuestion("상대방의 신체 부위 중 가장 매력적이라고 느끼는 곳은 어디야?", QuestionCategory.SPICY),
                createQuestion("우리가 나눈 스킨십 중 가장 기억에 남는 순간은 언제야?", QuestionCategory.SPICY),
                createQuestion("만약 우리가 하루 동안 영혼이 바뀐다면, 무엇을 제일 먼저 해보고 싶어?", QuestionCategory.DEEP_TALK),
                createQuestion("나를 만나고 나서 네 삶에서 가장 크게 변한 점이 있다면 뭐야?", QuestionCategory.DEEP_TALK),
                createQuestion("우리가 싸웠을 때, 내가 어떻게 화해를 청하면 기분이 풀릴 것 같아?", QuestionCategory.DEEP_TALK),
                createQuestion("서로의 은밀한 취향 중 하나를 더 알게 된다면 어떤 걸 알고 싶어?", QuestionCategory.SPICY)
            );

            questionRepository.saveAll(initialQuestions);
            log.info("{}개의 초기 질문 데이터가 생성되었습니다.", initialQuestions.size());
        }
    }

    private Question createQuestion(String content, QuestionCategory category) {
        return Question.builder()
                .content(content)
                .category(category)
                .build();
    }
}
