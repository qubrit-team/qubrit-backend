package qubrit_backend.recommend;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "추천", description = "맞춤 문제 세트 추천")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

	private final RecommendationService service;

	public RecommendationController(RecommendationService service) {
		this.service = service;
	}

	@Operation(summary = "맞춤 세트 추천",
			description = "내 풀이 기록(과목별 정답률), 관심 과목, 다른 사용자의 평가로 점수를 매겨 공개 세트를 추천합니다. 내가 만들었거나 이미 푼 세트는 제외됩니다. reason에 추천 이유가 담깁니다.")
	@GetMapping
	public List<RecommendationDto> recommend(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String subject, @RequestParam(defaultValue = "10") int size) {
		return service.recommend(userId, subject, size);
	}

	@Operation(summary = "학급 친구들이 많이 푼 세트",
			description = "나와 같은 학급에 속한 친구들이 많이 푼 공개 세트를 푼 친구 수 순으로 돌려줍니다. 학급이 없거나 친구들이 푼 세트가 없으면 빈 배열입니다.")
	@GetMapping("/classmates")
	public List<ClassmatePopularDto> classmatePopular(@AuthenticationPrincipal Long userId,
			@RequestParam(defaultValue = "10") int size) {
		return service.classmatePopular(userId, size);
	}
}
