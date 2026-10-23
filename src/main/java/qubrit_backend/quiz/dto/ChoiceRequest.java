package qubrit_backend.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "객관식 선택지")
public record ChoiceRequest(
		@NotBlank @Size(max = 500) String content,
		@Schema(description = "정답 여부 (객관식은 정확히 1개)") boolean correct) {
}
