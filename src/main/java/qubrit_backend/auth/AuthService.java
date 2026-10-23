package qubrit_backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.auth.domain.User;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.auth.dto.EmailCheckResponse;
import qubrit_backend.auth.dto.LoginRequest;
import qubrit_backend.auth.dto.SignupRequest;
import qubrit_backend.auth.dto.TokenResponse;
import qubrit_backend.auth.jwt.JwtProvider;

import java.util.List;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
	}

	@Transactional
	public void signup(SignupRequest req) {
		if (userRepository.existsByEmail(req.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
		}
		List<String> subjects = req.subjects() == null ? List.of() : req.subjects();
		userRepository.save(new User(req.email(), passwordEncoder.encode(req.password()), req.nickname(), subjects));
	}

	@Transactional(readOnly = true)
	public EmailCheckResponse checkEmail(String email) {
		return new EmailCheckResponse(!userRepository.existsByEmail(email));
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest req) {
		// 이메일/비밀번호 오류를 구분하지 않고 같은 메시지 반환
		User user = userRepository.findByEmail(req.email())
				.filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
						"이메일 또는 비밀번호가 올바르지 않습니다."));
		return new TokenResponse(jwtProvider.createToken(user.getId(), user.getEmail()),
				user.getEmail(), user.getNickname());
	}

	@Transactional(readOnly = true)
	public TokenResponse me(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		return new TokenResponse(null, user.getEmail(), user.getNickname());
	}
}
