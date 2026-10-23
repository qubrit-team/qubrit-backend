package qubrit_backend.quiz;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.quiz.dto.*;

@Tag(name = "문제 세트", description = "문제 세트/문제 출제와 조회")
@RestController
@RequestMapping("/api")
public class QuestionSetController {

	private final QuestionSetService service;

	public QuestionSetController(QuestionSetService service) {
		this.service = service;
	}

	@Operation(summary = "세트 생성", description = "문제를 함께 보낼 수 있습니다. 직접 출제한 세트는 바로 발행됩니다.")
	@PostMapping("/sets")
	@ResponseStatus(HttpStatus.CREATED)
	public SetDetail create(@AuthenticationPrincipal Long userId, @Valid @RequestBody SetRequest req) {
		return service.create(userId, req);
	}

	@Operation(summary = "세트 목록", description = "scope=mine(내가 만든 세트) 또는 public(공개 세트, 기본). subject, q(제목 검색)로 거를 수 있고, 공개 세트는 sort=latest(기본)/popular(풀이 많은 순)/trust(평가 좋은 순)로 정렬합니다.")
	@GetMapping("/sets")
	public PageResponse<SetSummary> list(@AuthenticationPrincipal Long userId,
			@RequestParam(defaultValue = "public") String scope,
			@RequestParam(required = false) String subject, @RequestParam(required = false) String q,
			@RequestParam(defaultValue = "latest") String sort,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.list(userId, scope, subject, q, sort, page, size);
	}

	@Operation(summary = "세트 상세", description = "출제자가 아니면 정답/해설은 내려가지 않습니다.")
	@GetMapping("/sets/{id}")
	public SetDetail get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.get(userId, id);
	}

	@Operation(summary = "공유 코드로 세트 조회")
	@GetMapping("/sets/code/{shareCode}")
	public SetDetail getByCode(@AuthenticationPrincipal Long userId, @PathVariable String shareCode) {
		return service.getByCode(userId, shareCode);
	}

	@Operation(summary = "세트 수정(제목/설명/과목/공개 범위)", description = "출제자만 가능합니다.")
	@PutMapping("/sets/{id}")
	public SetDetail update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
			@Valid @RequestBody SetRequest req) {
		return service.update(userId, id, req);
	}

	@Operation(summary = "세트 삭제", description = "출제자만 가능합니다.")
	@DeleteMapping("/sets/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		service.delete(userId, id);
	}

	@Operation(summary = "세트 발행", description = "DRAFT 세트를 발행합니다.")
	@PostMapping("/sets/{id}/publish")
	public SetDetail publish(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.publish(userId, id);
	}

	@Operation(summary = "문제 추가")
	@PostMapping("/sets/{id}/questions")
	@ResponseStatus(HttpStatus.CREATED)
	public QuestionDto addQuestion(@AuthenticationPrincipal Long userId, @PathVariable Long id,
			@Valid @RequestBody QuestionRequest req) {
		return service.addQuestion(userId, id, req);
	}

	@Operation(summary = "문제 수정")
	@PutMapping("/questions/{id}")
	public QuestionDto updateQuestion(@AuthenticationPrincipal Long userId, @PathVariable Long id,
			@Valid @RequestBody QuestionRequest req) {
		return service.updateQuestion(userId, id, req);
	}

	@Operation(summary = "문제 삭제")
	@DeleteMapping("/questions/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteQuestion(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		service.deleteQuestion(userId, id);
	}
}
