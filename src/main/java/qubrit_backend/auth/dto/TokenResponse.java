package qubrit_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// 로그인 성공 시 프론트에 돌려줄 값 (/me에서는 accessToken이 null)
@Schema(description = "로그인/내 정보 응답")
public record TokenResponse(
		@Schema(description = "JWT 액세스 토큰 (로그인 응답에만 포함, 이후 요청 헤더에 'Bearer {토큰}'으로 전달)")
		String accessToken,
		@Schema(description = "이메일", example = "qubrit@example.com")
		String email,
		@Schema(description = "닉네임", example = "큐브릿러")
		String nickname) {
}
