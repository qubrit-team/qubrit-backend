package qubrit_backend.review;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "문제 품질", description = "사용자 평가와 AI 검증")
@RestController
@RequestMapping("/api")
public class ReviewController {

	private final ReviewService service;

	public ReviewController(ReviewService service) {
		this.service = service;
	}

	@Operation(summary = "문제 평가(좋아요/신고)", description = "1인 1표, 다시 보내면 바뀝니다. 내가 만든 문제는 평가할 수 없습니다. 평가가 쌓이면 문제의 품질 상태가 바뀌고, FAIL이면 풀이 화면에서 숨겨집니다.")
	@PostMapping("/questions/{id}/votes")
	public QualitySummary vote(@AuthenticationPrincipal Long userId, @PathVariable Long id,
			@Valid @RequestBody VoteRequest req) {
		return service.vote(userId, id, req);
	}

	@Operation(summary = "문제 품질 조회", description = "좋아요/신고 수와 내 평가. 출제자에게는 품질 상태와 AI 검증 결과도 포함됩니다.")
	@GetMapping("/questions/{id}/quality")
	public QualitySummary quality(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.get(userId, id);
	}

	@Operation(summary = "AI 검증 실행(문제 1개)", description = "출제자만 가능합니다. 정답 정합성/모호성/표현을 Gemini가 점검해 PASS/WARN/FAIL로 판정합니다.")
	@PostMapping("/questions/{id}/review")
	public QualitySummary review(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.review(userId, id);
	}

	@Operation(summary = "AI 검증 이력", description = "출제자만 가능합니다. 최신순.")
	@GetMapping("/questions/{id}/reviews")
	public List<ReviewDto> history(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.history(userId, id);
	}

	@Operation(summary = "AI 검증 실행(세트 전체)", description = "출제자만 가능합니다. 문제 수만큼 Gemini를 호출하므로 오래 걸릴 수 있습니다.")
	@PostMapping("/sets/{id}/review")
	public List<QualitySummary> reviewSet(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.reviewSet(userId, id);
	}
}
