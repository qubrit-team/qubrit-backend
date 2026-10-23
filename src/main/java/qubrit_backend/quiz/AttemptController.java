package qubrit_backend.quiz;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.quiz.domain.AttemptStatus;
import qubrit_backend.quiz.dto.*;

@Tag(name = "풀이", description = "문제 세트 풀이/채점")
@RestController
@RequestMapping("/api")
public class AttemptController {

	private final AttemptService service;

	public AttemptController(AttemptService service) {
		this.service = service;
	}

	@Operation(summary = "풀이 시작", description = "진행 중인 풀이가 있으면 그것을 이어서 반환합니다.")
	@PostMapping("/sets/{id}/attempts")
	public AttemptDto start(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.start(userId, id);
	}

	@Operation(summary = "답 제출", description = "문제당 1번만 제출할 수 있고, 제출 즉시 채점 결과와 해설을 반환합니다.")
	@PutMapping("/attempts/{id}/answers/{questionId}")
	public AnswerResult answer(@AuthenticationPrincipal Long userId, @PathVariable Long id,
			@PathVariable Long questionId, @Valid @RequestBody AnswerRequest req) {
		return service.answer(userId, id, questionId, req);
	}

	@Operation(summary = "풀이 종료", description = "점수를 확정합니다. 이미 종료된 풀이면 그대로 반환합니다.")
	@PostMapping("/attempts/{id}/finish")
	public AttemptDto finish(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.finish(userId, id);
	}

	@Operation(summary = "풀이 조회")
	@GetMapping("/attempts/{id}")
	public AttemptDto get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.get(userId, id);
	}

	@Operation(summary = "내 풀이 기록", description = "status=IN_PROGRESS면 '이어서 풀기' 목록입니다. 최근순.")
	@GetMapping("/me/attempts")
	public List<AttemptSummary> mine(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) AttemptStatus status, @RequestParam(defaultValue = "20") int size) {
		return service.mine(userId, status, size);
	}
}
