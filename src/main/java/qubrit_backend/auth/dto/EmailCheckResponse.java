package qubrit_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 중복확인 응답")
public record EmailCheckResponse(
		@Schema(description = "사용 가능 여부 (true면 가입 가능)", example = "true")
		boolean available) {
}
