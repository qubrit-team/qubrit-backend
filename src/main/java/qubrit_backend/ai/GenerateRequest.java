package qubrit_backend.ai;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import qubrit_backend.quiz.domain.QuestionType;

@Schema(description = "AI 출제 옵션. 모두 생략 가능 (기본: 5문제, 객관식+주관식, 보통)")
public record GenerateRequest(
		@Min(1) @Max(20) Integer count,
		List<QuestionType> types,
		Difficulty difficulty) {
}
