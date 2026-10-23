package qubrit_backend.quiz;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.ai.AnswerGrader;
import qubrit_backend.quiz.domain.*;
import qubrit_backend.quiz.dto.*;

@Service
public class AttemptService {

	private final QuestionSetService setService;
	private final AttemptRepository attemptRepository;
	private final AttemptAnswerRepository answerRepository;
	private final WrongNoteRepository wrongNoteRepository;
	private final AnswerGrader grader;
	private final QuestionRepository questionRepository;
	private final SetMetricsService metrics;

	public AttemptService(QuestionSetService setService, AttemptRepository attemptRepository,
			AttemptAnswerRepository answerRepository, WrongNoteRepository wrongNoteRepository, AnswerGrader grader,
			QuestionRepository questionRepository, SetMetricsService metrics) {
		this.metrics = metrics;
		this.grader = grader;
		this.questionRepository = questionRepository;
		this.setService = setService;
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.wrongNoteRepository = wrongNoteRepository;
	}

	/** 진행 중인 풀이가 있으면 그걸 이어서 반환한다. */
	@Transactional
	public AttemptDto start(Long userId, Long setId) {
		QuestionSet set = setService.requireViewable(userId, setId);
		if (set.getStatus() != SetStatus.PUBLISHED && !set.isOwner(userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문제 세트를 찾을 수 없습니다.");
		}
		if (set.visibleQuestions().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "풀 수 있는 문제가 없는 세트입니다.");
		}
		Attempt attempt = attemptRepository
				.findFirstByUserIdAndQuestionSetIdAndStatus(userId, setId, AttemptStatus.IN_PROGRESS)
				.orElseGet(() -> attemptRepository.save(new Attempt(userId, set)));
		return toDto(attempt);
	}

	@Transactional
	public AnswerResult answer(Long userId, Long attemptId, Long questionId, AnswerRequest req) {
		Attempt attempt = requireOwned(userId, attemptId);
		if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 끝난 풀이입니다.");
		}
		Question q = attempt.getQuestionSet().getQuestions().stream()
				.filter(x -> x.getId().equals(questionId)).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."));
		if (q.getQualityStatus() == QualityStatus.FAIL) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다.");
		}
		if (answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 답을 제출한 문제입니다.");
		}

		Grade grade = grade(q, req);
		answerRepository.save(new AttemptAnswer(attempt, q, req.selectedChoiceId(), req.answerText(),
				grade.correct(), grade.feedback()));
		updateWrongNote(userId, q, grade.correct());
		return new AnswerResult(grade.correct(), correctChoiceId(q), correctAnswer(q), q.getExplanation(),
				grade.feedback());
	}

	@Transactional
	public AttemptDto finish(Long userId, Long attemptId) {
		Attempt attempt = requireOwned(userId, attemptId);
		if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
			int score = (int) answerRepository.findByAttemptId(attemptId).stream()
					.filter(AttemptAnswer::isCorrect).count();
			attempt.finish(score);
			metrics.refresh(attempt.getQuestionSet());
		}
		return toDto(attempt);
	}

	@Transactional(readOnly = true)
	public AttemptDto get(Long userId, Long attemptId) {
		return toDto(requireOwned(userId, attemptId));
	}

	@Transactional(readOnly = true)
	public List<AttemptSummary> mine(Long userId, AttemptStatus status, int size) {
		PageRequest page = PageRequest.of(0, Math.min(Math.max(size, 1), 50));
		List<Attempt> attempts = status == null
				? attemptRepository.findByUserIdOrderByStartedAtDesc(userId, page)
				: attemptRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, status, page);
		return attempts.stream().map(a -> new AttemptSummary(a.getId(), a.getQuestionSet().getId(),
				a.getQuestionSet().getTitle(), a.getQuestionSet().getSubject().getName(), a.getStatus(),
				a.getTotal(), answerRepository.findByAttemptId(a.getId()).size(), a.getScore(),
				a.getStartedAt())).toList();
	}

	// --- 채점 ---

	private record Grade(boolean correct, String feedback) {
	}

	private Grade grade(Question q, AnswerRequest req) {
		if (q.getType() == QuestionType.MCQ) {
			if (req.selectedChoiceId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택지를 골라 주세요.");
			}
			QuestionChoice picked = q.getChoices().stream()
					.filter(c -> c.getId().equals(req.selectedChoiceId())).findFirst()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 선택지입니다."));
			return new Grade(picked.isCorrect(), null);
		}
		if (req.answerText() == null || req.answerText().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답을 입력해 주세요.");
		}
		// 정확히 일치하면 AI를 부르지 않는다. 다르면 출제자의 정답을 기준으로 AI가 판단하고,
		// AI를 쓸 수 없을 때는 오답으로 처리한다.
		if (normalize(req.answerText()).equals(normalize(q.getAnswerText()))) {
			return new Grade(true, null);
		}
		return grader.grade(q.getContent(), q.getAnswerText(), req.answerText())
				.map(v -> new Grade(v.correct(), v.feedback()))
				.orElse(new Grade(false, null));
	}

	private static String normalize(String s) {
		return s.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
	}

	private void updateWrongNote(Long userId, Question answered, boolean correct) {
		// 오답 노트 복습용으로 복사된 문제는 원본 문제의 오답 노트를 갱신한다
		Question q = answered.getOriginQuestionId() == null ? answered
				: questionRepository.findById(answered.getOriginQuestionId()).orElse(null);
		if (q == null) {
			return; // 원본이 삭제됨
		}
		Question target = q;
		var note = wrongNoteRepository.findByUserIdAndQuestionId(userId, q.getId());
		if (correct) {
			note.ifPresent(WrongNote::markResolved);
		} else {
			WrongNote n = note.orElseGet(() -> new WrongNote(userId, target));
			n.markWrong();
			wrongNoteRepository.save(n);
		}
	}

	private Long correctChoiceId(Question q) {
		return q.getChoices().stream().filter(QuestionChoice::isCorrect).map(QuestionChoice::getId)
				.findFirst().orElse(null);
	}

	private String correctAnswer(Question q) {
		if (q.getType() == QuestionType.SHORT) {
			return q.getAnswerText();
		}
		return q.getChoices().stream().filter(QuestionChoice::isCorrect).map(QuestionChoice::getContent)
				.findFirst().orElse(null);
	}

	// --- 변환 ---

	private AttemptDto toDto(Attempt a) {
		List<AttemptDto.AttemptAnswerDto> answers = answerRepository.findByAttemptId(a.getId()).stream()
				.map(x -> new AttemptDto.AttemptAnswerDto(x.getQuestion().getId(), x.getSelectedChoiceId(),
						x.getAnswerText(), x.isCorrect(), correctChoiceId(x.getQuestion()),
						correctAnswer(x.getQuestion()), x.getQuestion().getExplanation(), x.getAiFeedback()))
				.toList();
		return new AttemptDto(a.getId(), a.getQuestionSet().getId(), a.getQuestionSet().getTitle(),
				a.getQuestionSet().getSubject().getName(), a.getStatus(), a.getTotal(), answers.size(),
				a.getScore(), a.getStartedAt(), a.getFinishedAt(), answers);
	}

	private Attempt requireOwned(Long userId, Long attemptId) {
		Attempt a = attemptRepository.findById(attemptId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "풀이를 찾을 수 없습니다."));
		if (!a.getUserId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "풀이를 찾을 수 없습니다.");
		}
		return a;
	}
}
