package qubrit_backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.auth.dto.EmailCheckResponse;
import qubrit_backend.auth.dto.LoginRequest;
import qubrit_backend.auth.dto.SignupRequest;
import qubrit_backend.auth.dto.TokenResponse;

@Tag(name = "인증", description = "회원가입 / 로그인 / 내 정보 조회")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "회원가입", description = "이메일, 비밀번호(영문+숫자+특수문자, 8자 이상), 닉네임(2~10자), 관심 과목으로 가입합니다.")
	@ApiResponse(responseCode = "201", description = "가입 성공")
	@ApiResponse(responseCode = "400", description = "입력값 오류 (이메일 형식, 비밀번호 8자 미만 등)")
	@ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public void signup(@Valid @RequestBody SignupRequest req) {
		authService.signup(req);
	}

	@Operation(summary = "이메일 중복확인", description = "available이 true면 가입에 사용할 수 있는 이메일입니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/check-email")
	public EmailCheckResponse checkEmail(@RequestParam String email) {
		return authService.checkEmail(email);
	}

	@Operation(summary = "로그인", description = "성공하면 accessToken을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "로그인 성공")
	@ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest req) {
		return authService.login(req);
	}

	// JWT 필터가 등록한 userId
	@Operation(summary = "내 정보 조회", description = "Authorization 헤더의 토큰으로 현재 로그인한 사용자를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않음")
	@GetMapping("/me")
	public TokenResponse me(@AuthenticationPrincipal Long userId) {
		return authService.me(userId);
	}
}
