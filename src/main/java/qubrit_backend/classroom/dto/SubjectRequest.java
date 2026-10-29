package qubrit_backend.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "학급 과목 추가 요청")
public record SubjectRequest(
		@Schema(description = "추가할 과목 이름 (1~30자)", example = "네트워크") @NotBlank @Size(max = 30) String name) {
}
