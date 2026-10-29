package qubrit_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "회원가입 요청")
public record SignupRequest(
		@Schema(description = "이메일 (로그인 ID, 중복 불가)", example = "qubrit@example.com")
		@NotBlank @Email String email,
		@Schema(description = "비밀번호 (영문+숫자+특수문자 포함, 8~64자)", example = "password1234!")
		@NotBlank @Size(min = 8, max = 64)
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$") String password,
		@Schema(description = "닉네임 (2~10자)", example = "큐브릿러")
		@NotBlank @Size(min = 2, max = 10) String nickname,
		@Schema(description = "관심 과목 (복수 선택, 선택 안 하면 빈 배열)", example = "[\"국어\", \"수학\"]")
		@Size(max = 10) List<String> subjects) {
}
