package qubrit_backend.study;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.quiz.AttemptService;
import qubrit_backend.quiz.domain.AttemptStatus;
import qubrit_backend.quiz.dto.AttemptDto;
import qubrit_backend.quiz.dto.PageResponse;
import qubrit_backend.tutor.TutorService;

@Tag(name = "홈/학습 현황", description = "홈 화면, 주간 목표, 오답 노트")
@RestController
@RequestMapping("/api")
public class StudyController {

	private final StudyStatsService statsService;
	private final TutorService tutorService;
	private final AttemptService attemptService;
	private final WrongNoteService wrongNoteService;
	private final UserRepository userRepository;

	public StudyController(StudyStatsService statsService, TutorService tutorService, AttemptService attemptService,
			WrongNoteService wrongNoteService, UserRepository userRepository) {
		this.statsService = statsService;
		this.tutorService = tutorService;
		this.attemptService = attemptService;
		this.wrongNoteService = wrongNoteService;
		this.userRepository = userRepository;
	}

	@Operation(summary = "홈 화면 데이터", description = "닉네임, 과외쌤의 오늘의 추천, 주간 목표 진행, 연속 학습 일수, 오답 수, 이어서 풀 목록을 한 번에 반환합니다. AI를 쓸 수 없어도 규칙 기반 문구로 응답합니다.")
	@GetMapping("/home")
	public HomeResponse home(@AuthenticationPrincipal Long userId) {
		StudyStats stats = statsService.compute(userId);
		String nickname = userRepository.findById(userId).map(u -> u.getNickname()).orElse("");
		return new HomeResponse(nickname, tutorService.today(userId, stats),
				new HomeResponse.GoalProgress(stats.weeklyDone(), stats.weeklyGoal()), stats.streak(),
				stats.unresolvedWrong(), attemptService.mine(userId, AttemptStatus.IN_PROGRESS, 5));
	}

	@Operation(summary = "주간 목표 변경", description = "한 주(월~일)에 풀 문제 수를 1~200 사이로 정합니다.")
	@PutMapping("/me/goal")
	@Transactional
	public void changeGoal(@AuthenticationPrincipal Long userId, @Valid @RequestBody GoalRequest req) {
		userRepository.findById(userId).orElseThrow().changeWeeklyGoal(req.weeklyGoal());
	}

	@Operation(summary = "오답 노트", description = "틀린 문제 목록(최근순). resolved=true면 이후 맞혀서 해결된 문제입니다.")
	@GetMapping("/wrong-notes")
	public PageResponse<WrongNoteDto> wrongNotes(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String subject, @RequestParam(defaultValue = "false") boolean resolved,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return wrongNoteService.list(userId, subject, resolved, page, size);
	}

	@Operation(summary = "오답 복습 시작", description = "해결하지 못한 오답으로 복습 세트를 만들고 풀이를 바로 시작합니다. 응답은 풀이 시작 응답과 같고, 이후 /api/attempts/{id}/answers/{questionId}로 풉니다. 복습에서 맞히면 오답 노트에서 해결 처리됩니다.")
	@PostMapping("/wrong-notes/retry")
	@ResponseStatus(HttpStatus.CREATED)
	public AttemptDto retry(@AuthenticationPrincipal Long userId, @Valid @RequestBody(required = false) RetryRequest req) {
		RetryRequest r = req == null ? new RetryRequest(null, null) : req;
		return wrongNoteService.retry(userId, r.subject(), r.size());
	}
}
