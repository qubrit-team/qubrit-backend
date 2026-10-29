package qubrit_backend.study;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.quiz.AttemptService;
import qubrit_backend.quiz.QuestionSetService;
import qubrit_backend.quiz.domain.Question;
import qubrit_backend.quiz.domain.QuestionSet;
import qubrit_backend.quiz.domain.WrongNote;
import qubrit_backend.quiz.domain.WrongNoteRepository;
import qubrit_backend.quiz.dto.AttemptDto;
import qubrit_backend.quiz.dto.PageResponse;

@Service
public class WrongNoteService {

	private static final int DEFAULT_RETRY_SIZE = 10;

	private final WrongNoteRepository wrongNoteRepository;
	private final QuestionSetService setService;
	private final AttemptService attemptService;

	public WrongNoteService(WrongNoteRepository wrongNoteRepository, QuestionSetService setService,
			AttemptService attemptService) {
		this.wrongNoteRepository = wrongNoteRepository;
		this.setService = setService;
		this.attemptService = attemptService;
	}

	@Transactional(readOnly = true)
	public PageResponse<WrongNoteDto> list(Long userId, String subject, boolean resolved, int page, int size) {
		var result = wrongNoteRepository.findList(userId, subject == null ? "" : subject.trim(), resolved,
				PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
		return PageResponse.of(result, w -> {
			Question q = w.getQuestion();
			return new WrongNoteDto(w.getId(), q.getId(), q.getQuestionSet().getId(), q.getQuestionSet().getTitle(),
					q.getQuestionSet().getSubject().getName(), q.getType(), q.getContent(), w.getWrongCount(),
					w.isResolved(), w.getLastWrongAt());
		});
	}

	/**
	 * 아직 해결하지 못한 오답 문제로 복습 세트를 만들고 풀이를 시작한다.
	 * subject를 주지 않으면 해결 못 한 오답이 가장 많은 과목을 고른다.
	 */
	@Transactional
	public AttemptDto retry(Long userId, String subject, Integer size) {
		String filter = subject == null ? "" : subject.trim();
		List<WrongNote> candidates = wrongNoteRepository.findRetryCandidates(userId, filter);
		if (candidates.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "복습할 오답이 없어요.");
		}
		String target = filter;
		if (target.isEmpty()) {
			Map<String, Long> bySubject = candidates.stream().collect(
					Collectors.groupingBy(w -> w.getQuestion().getQuestionSet().getSubject().getName(), Collectors.counting()));
			target = bySubject.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
		}
		String subjectName = target;
		List<Question> picked = candidates.stream()
				.filter(w -> w.getQuestion().getQuestionSet().getSubject().getName().equals(subjectName))
				.limit(size == null ? DEFAULT_RETRY_SIZE : Math.min(Math.max(size, 1), 30))
				.map(WrongNote::getQuestion).toList();
		QuestionSet set = setService.createReviewSet(userId, subjectName, "오답 노트 복습 (" + subjectName + ")", picked);
		return attemptService.start(userId, set.getId());
	}
}
