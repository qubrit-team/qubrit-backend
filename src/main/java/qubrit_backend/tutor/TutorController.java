package qubrit_backend.tutor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "과외쌤 AI", description = "오늘의 추천과 학습 분석")
@RestController
@RequestMapping("/api/tutor")
public class TutorController {

	private final TutorService service;

	public TutorController(TutorService service) {
		this.service = service;
	}

	@Operation(summary = "오늘의 추천", description = "풀이 기록을 바탕으로 오늘 풀 과목/문제 수와 응원 메시지를 줍니다. AI 응답은 하루 한 번 만들어 저장하고, AI를 쓸 수 없으면 ai=false인 규칙 기반 문구가 옵니다.")
	@GetMapping("/today")
	public TutorToday today(@AuthenticationPrincipal Long userId) {
		return service.today(userId);
	}

	@Operation(summary = "학습 분석", description = "과목별 정답률(항상 최신)과, AI의 총평/약점/조언(하루 한 번 생성). AI를 쓸 수 없으면 ai=false인 규칙 기반 요약이 옵니다.")
	@GetMapping("/analysis")
	public TutorAnalysis analysis(@AuthenticationPrincipal Long userId) {
		return service.analysis(userId);
	}
}
