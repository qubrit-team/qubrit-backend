package qubrit_backend.quiz;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.auth.domain.User;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.quiz.domain.*;
import qubrit_backend.quiz.dto.*;
import qubrit_backend.review.QualityService;
import qubrit_backend.review.ReviewDto;
import qubrit_backend.subject.Subject;
import qubrit_backend.subject.SubjectRepository;

@Service
public class QuestionSetService {

	// 헷갈리는 글자(0/O, 1/I)는 뺀 공유 코드 문자
	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final QuestionSetRepository setRepository;
	private final QuestionRepository questionRepository;
	private final SubjectRepository subjectRepository;
	private final UserRepository userRepository;
	private final AttemptRepository attemptRepository;
	private final AttemptAnswerRepository answerRepository;
	private final WrongNoteRepository wrongNoteRepository;
	private final QualityService quality;
	private final SetMetricsService metrics;

	public QuestionSetService(QuestionSetRepository setRepository, QuestionRepository questionRepository,
			SubjectRepository subjectRepository, UserRepository userRepository, AttemptRepository attemptRepository,
			AttemptAnswerRepository answerRepository, WrongNoteRepository wrongNoteRepository,
			QualityService quality, SetMetricsService metrics) {
		this.quality = quality;
		this.metrics = metrics;
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.wrongNoteRepository = wrongNoteRepository;
		this.setRepository = setRepository;
		this.questionRepository = questionRepository;
		this.subjectRepository = subjectRepository;
		this.userRepository = userRepository;
	}

	// 5문항 미만의 세트는 등록할 수 없다
	static final int MIN_QUESTIONS = 5;

	private static void requireMinQuestions(int count) {
		if (count < MIN_QUESTIONS) {
			throw bad("문제는 " + MIN_QUESTIONS + "개 이상이어야 등록할 수 있습니다.");
		}
	}

	@Transactional
	public SetDetail create(Long userId, SetRequest req) {
		requireMinQuestions(req.questions() == null ? 0 : req.questions().size());
		QuestionSet set = new QuestionSet(userId, findSubject(req.subject()), req.title().trim(),
				req.description(), newShareCode(), req.visibility() == null ? Visibility.PUBLIC : req.visibility(),
				SetSource.MANUAL, SetStatus.PUBLISHED);
		if (req.questions() != null) {
			for (QuestionRequest qr : req.questions()) {
				set.addQuestion(buildQuestion(set, qr));
			}
		}
		setRepository.save(set);
		metrics.refresh(set);
		return detail(set, userId);
	}

	/** AI가 만든 문제로 초안 세트를 만든다. 형식이 맞지 않는 문제는 건너뛰고, 하나도 남지 않으면 400. */
	@Transactional
	public SetDetail createDraft(Long userId, String subject, String title, Long materialId,
			List<QuestionRequest> generated) {
		QuestionSet set = new QuestionSet(userId, findSubject(subject), title, null, newShareCode(),
				Visibility.PUBLIC, SetSource.AI, SetStatus.DRAFT);
		set.linkMaterial(materialId);
		for (QuestionRequest qr : generated) {
			try {
				set.addQuestion(buildQuestion(set, qr));
			} catch (ResponseStatusException skipped) {
				// AI가 규칙에 어긋난 문제를 만든 경우
			}
		}
		if (set.getQuestions().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI가 쓸 수 있는 문제를 만들지 못했습니다. 다시 시도해 주세요.");
		}
		setRepository.save(set);
		metrics.refresh(set);
		return detail(set, userId);
	}

	/** 오답 노트 복습용 개인 세트: 원본 문제를 복사해 비공개로 만든다. */
	@Transactional
	public QuestionSet createReviewSet(Long userId, String subject, String title, List<Question> sources) {
		QuestionSet set = new QuestionSet(userId, findSubject(subject), title, null, newShareCode(),
				Visibility.PRIVATE, SetSource.MANUAL, SetStatus.PUBLISHED);
		set.markReviewSet();
		for (Question src : sources) {
			Question copy = new Question(set, src.getType(), src.getContent(), src.getPassage(), src.getExplanation(),
					src.getAnswerText());
			copy.copiedFrom(src.getId());
			for (QuestionChoice c : src.getChoices()) {
				copy.addChoice(new QuestionChoice(copy, c.getContent(), c.isCorrect()));
			}
			set.addQuestion(copy);
		}
		return setRepository.save(set);
	}

	@Transactional(readOnly = true)
	public PageResponse<SetSummary> list(Long userId, String scope, String subject, String q, String sort, int page, int size) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
		String s = subject == null ? "" : subject.trim();
		String keyword = q == null ? "" : q.trim();
		Page<QuestionSet> result = "mine".equals(scope)
				? setRepository.findMine(userId, s, keyword, pageable)
				: switch (sort == null ? "" : sort) {
					case "popular" -> setRepository.findPublicByPopularity(s, keyword, pageable);
					case "trust" -> setRepository.findPublicByTrust(s, keyword, pageable);
					default -> setRepository.findPublic(s, keyword, pageable);
				};
		Map<Long, String> nicknames = nicknames(result.getContent().stream().map(QuestionSet::getOwnerId).toList());
		return PageResponse.of(result, set -> summary(set, userId, nicknames.get(set.getOwnerId())));
	}

	@Transactional(readOnly = true)
	public SetDetail get(Long userId, Long id) {
		return detail(requireViewable(userId, id), userId);
	}

	@Transactional(readOnly = true)
	public SetDetail getByCode(Long userId, String code) {
		QuestionSet set = setRepository.findByShareCode(code.trim().toUpperCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문제 세트를 찾을 수 없습니다."));
		return detail(requireViewable(userId, set.getId()), userId);
	}

	@Transactional
	public SetDetail update(Long userId, Long id, SetRequest req) {
		QuestionSet set = requireOwned(userId, id);
		set.update(findSubject(req.subject()), req.title().trim(), req.description(),
				req.visibility() == null ? set.getVisibility() : req.visibility());
		return detail(set, userId);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		QuestionSet set = requireOwned(userId, id);
		// 풀이 기록/오답 노트가 문제를 참조하므로 먼저 지운다
		answerRepository.deleteBySetId(id);
		wrongNoteRepository.deleteBySetId(id);
		quality.deleteForSet(id);
		attemptRepository.deleteBySetId(id);
		setRepository.delete(set);
	}

	@Transactional
	public SetDetail publish(Long userId, Long id) {
		QuestionSet set = requireOwned(userId, id);
		requireMinQuestions(set.getQuestions().size());
		set.publish();
		metrics.refresh(set);
		return detail(set, userId);
	}

	@Transactional
	public QuestionDto addQuestion(Long userId, Long setId, QuestionRequest req) {
		QuestionSet set = requireOwned(userId, setId);
		Question q = buildQuestion(set, req);
		set.addQuestion(q);
		questionRepository.save(q);
		metrics.refresh(set);
		return toDto(q, true, QualityService.Stats.empty());
	}

	@Transactional
	public QuestionDto updateQuestion(Long userId, Long questionId, QuestionRequest req) {
		requireOwnedQuestion(userId, questionId);
		validate(req);
		// 내용이 바뀌면 이전 평가/AI 검증은 의미가 없어진다 (삭제 쿼리가 영속성 컨텍스트를 비우므로 다시 읽는다)
		quality.deleteForQuestion(questionId);
		Question q = questionRepository.findById(questionId).orElseThrow();
		q.update(req.type(), req.content().trim(), blankToNull(req.passage()), req.explanation(), answerTextOf(req));
		q.clearChoices();
		questionRepository.flush(); // orphan 삭제를 먼저 반영
		addChoices(q, req);
		metrics.refresh(q.getQuestionSet());
		return toDto(q, true, QualityService.Stats.empty());
	}

	@Transactional
	public void deleteQuestion(Long userId, Long questionId) {
		Question q = requireOwnedQuestion(userId, questionId);
		answerRepository.deleteByQuestionId(questionId);
		wrongNoteRepository.deleteByQuestionId(questionId);
		quality.deleteForQuestion(questionId);
		// 위 삭제 쿼리가 영속성 컨텍스트를 비우므로 다시 읽어서 번호를 재정렬한다
		Question fresh = questionRepository.findById(questionId).orElseThrow();
		fresh.getQuestionSet().removeQuestion(fresh);
		metrics.refresh(fresh.getQuestionSet());
	}

	/** 열람 가능한 세트만 반환: 소유자이거나, 발행됨 + PRIVATE 아님. */
	@Transactional(readOnly = true)
	public QuestionSet requireViewable(Long userId, Long id) {
		QuestionSet set = setRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문제 세트를 찾을 수 없습니다."));
		boolean viewable = set.isOwner(userId)
				|| (set.getStatus() == SetStatus.PUBLISHED && set.getVisibility() != Visibility.PRIVATE);
		if (!viewable) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문제 세트를 찾을 수 없습니다.");
		}
		return set;
	}

	/** 여러 세트를 목록용 요약으로 바꾼다 (출제자 닉네임을 한 번에 조회). */
	@Transactional(readOnly = true)
	public List<SetSummary> summaries(List<QuestionSet> sets, Long userId) {
		Map<Long, String> nicknames = nicknames(sets.stream().map(QuestionSet::getOwnerId).toList());
		return sets.stream().map(s -> summary(s, userId, nicknames.get(s.getOwnerId()))).toList();
	}

	// --- 변환 ---

	public SetDetail detail(QuestionSet set, Long userId) {
		boolean mine = set.isOwner(userId);
		String nickname = nicknames(List.of(set.getOwnerId())).get(set.getOwnerId());
		var stats = quality.statsForSet(set.getId(), userId);
		// 출제자는 전부, 풀이자는 품질 검증을 통과한(FAIL이 아닌) 문제만 본다
		var shown = mine ? set.getQuestions() : set.visibleQuestions();
		return new SetDetail(summary(set, userId, nickname),
				shown.stream().map(q -> toDto(q, mine, stats.getOrDefault(q.getId(), QualityService.Stats.empty()))).toList(),
				stats(set.getId(), stats));
	}

	// 풀이 수 / 평균 정답률(맞힌 문제 ÷ 출제된 문제) / 신뢰도(👍 ÷ 전체 평가). 근거가 없으면 null
	private SetStats stats(Long setId, Map<Long, QualityService.Stats> questionStats) {
		var totals = attemptRepository.totalsForSet(setId);
		Integer accuracy = totals.getTotal() == 0 ? null : (int) Math.round(totals.getScore() * 100.0 / totals.getTotal());
		int up = questionStats.values().stream().mapToInt(QualityService.Stats::up).sum();
		int down = questionStats.values().stream().mapToInt(QualityService.Stats::down).sum();
		Integer trust = up + down == 0 ? null : (int) Math.round(up * 100.0 / (up + down));
		return new SetStats(totals.getCount(), accuracy, trust);
	}

	private SetSummary summary(QuestionSet set, Long userId, String ownerNickname) {
		return new SetSummary(set.getId(), set.getTitle(), set.getDescription(), set.getSubject().getName(),
				ownerNickname, set.isOwner(userId) ? set.getQuestions().size() : set.visibleQuestions().size(),
				set.getVisibility(), set.getStatus(), set.getSource(),
				set.getShareCode(), set.isOwner(userId), set.getCreatedAt(), metrics.view(set));
	}

	private QuestionDto toDto(Question q, boolean withAnswer, QualityService.Stats stats) {
		List<ChoiceDto> choices = q.getChoices().stream()
				.map(c -> new ChoiceDto(c.getId(), c.getContent(), withAnswer ? c.isCorrect() : null))
				.toList();
		return new QuestionDto(q.getId(), q.getPosition(), q.getType(), q.getContent(), q.getPassage(),
				withAnswer ? q.getExplanation() : null, withAnswer ? q.getAnswerText() : null,
				withAnswer ? q.getQualityStatus() : null, choices, stats.up(), stats.down(), stats.myVote(),
				withAnswer && stats.latestReview() != null ? ReviewDto.of(stats.latestReview()) : null);
	}

	private Map<Long, String> nicknames(List<Long> userIds) {
		return userRepository.findAllById(userIds.stream().distinct().toList()).stream()
				.collect(Collectors.toMap(User::getId, User::getNickname));
	}

	// --- 검증/생성 ---

	private Question buildQuestion(QuestionSet set, QuestionRequest req) {
		validate(req);
		Question q = new Question(set, req.type(), req.content().trim(), blankToNull(req.passage()), req.explanation(),
				answerTextOf(req));
		addChoices(q, req);
		return q;
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private String answerTextOf(QuestionRequest req) {
		return req.type() == QuestionType.SHORT ? req.answerText().trim() : null;
	}

	private void addChoices(Question q, QuestionRequest req) {
		if (req.type() != QuestionType.MCQ) {
			return;
		}
		for (ChoiceRequest c : req.choices()) {
			q.addChoice(new QuestionChoice(q, c.content().trim(), c.correct()));
		}
	}

	private void validate(QuestionRequest req) {
		if (req.type() == QuestionType.MCQ) {
			List<ChoiceRequest> choices = req.choices();
			if (choices == null || choices.size() < 2 || choices.size() > 6) {
				throw bad("객관식은 선택지가 2~6개여야 합니다.");
			}
			if (choices.stream().filter(ChoiceRequest::correct).count() != 1) {
				throw bad("객관식은 정답이 정확히 1개여야 합니다.");
			}
		} else if (req.answerText() == null || req.answerText().isBlank()) {
			throw bad("주관식은 정답을 입력해야 합니다.");
		}
	}

	private Subject findSubject(String name) {
		return subjectRepository.findByName(name.trim()).orElseThrow(() -> bad("존재하지 않는 과목입니다."));
	}

	private QuestionSet requireOwned(Long userId, Long id) {
		QuestionSet set = setRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문제 세트를 찾을 수 없습니다."));
		if (!set.isOwner(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "출제자만 할 수 있습니다.");
		}
		return set;
	}

	private Question requireOwnedQuestion(Long userId, Long questionId) {
		Question q = questionRepository.findById(questionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."));
		if (!q.getQuestionSet().isOwner(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "출제자만 할 수 있습니다.");
		}
		return q;
	}

	private String newShareCode() {
		while (true) {
			StringBuilder sb = new StringBuilder(6);
			for (int i = 0; i < 6; i++) {
				sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
			}
			if (!setRepository.existsByShareCode(sb.toString())) {
				return sb.toString();
			}
		}
	}

	private static ResponseStatusException bad(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}
