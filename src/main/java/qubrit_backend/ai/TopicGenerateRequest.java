package qubrit_backend.ai;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import qubrit_backend.quiz.domain.QuestionType;

@Schema(description = "자료 없이 주제만으로 AI 출제")
public record TopicGenerateRequest(
		@NotBlank String subject,
		@NotBlank @Size(max = 200) String topic,
		@Min(1) @Max(20) Integer count,
		List<QuestionType> types,
		Difficulty difficulty) {
}
