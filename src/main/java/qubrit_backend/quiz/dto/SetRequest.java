package qubrit_backend.quiz.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import qubrit_backend.quiz.domain.Visibility;

@Schema(description = "문제 세트 생성/수정 요청 (수정 시 questions는 무시)")
public record SetRequest(
		@NotBlank @Size(max = 100) String title,
		@Size(max = 500) String description,
		@Schema(description = "과목 이름", example = "국어") @NotBlank String subject,
		@Schema(description = "공개 범위, 생략하면 PUBLIC") Visibility visibility,
		@Valid @Size(max = 100) List<QuestionRequest> questions) {
}
