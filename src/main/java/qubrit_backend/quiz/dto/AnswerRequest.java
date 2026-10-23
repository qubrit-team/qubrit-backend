package qubrit_backend.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "답 제출. 객관식은 selectedChoiceId, 주관식은 answerText")
public record AnswerRequest(Long selectedChoiceId, @Size(max = 500) String answerText) {
}
