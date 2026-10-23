package qubrit_backend.quiz.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import qubrit_backend.quiz.domain.QuestionType;

@Schema(description = "문제 생성/수정 요청. MCQ는 선택지 2~6개(정답 1개), SHORT는 answerText 필수")
public record QuestionRequest(
		@NotNull QuestionType type,
		@NotBlank @Size(max = 2000) String content,
		@Schema(description = "지문(긴 글), 없으면 생략") @Size(max = 4000) String passage,
		@Size(max = 2000) String explanation,
		@Size(max = 500) String answerText,
		@Valid @Size(max = 6) List<ChoiceRequest> choices) {
}
