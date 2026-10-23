package qubrit_backend.review;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.ai.QuestionReviewer;
import qubrit_backend.quiz.QuestionSetService;
import qubrit_backend.quiz.SetMetricsService;
import qubrit_backend.quiz.domain.Question;
import qubrit_backend.quiz.domain.QuestionRepository;
import qubrit_backend.quiz.domain.QuestionSet;
import qubrit_backend.quiz.domain.SetStatus;

@Service
public class ReviewService {

	private final QuestionRepository questionRepository;
	private final QuestionSetService setService;
	private final QuestionVoteRepository voteRepository;
	private final QuestionReviewRepository reviewRepository;
	private final QualityService quality;
	private final QuestionReviewer reviewer;
	private final SetMetricsService metrics;

	public ReviewService(QuestionRepository questionRepository, QuestionSetService setService,
			QuestionVoteRepository voteRepository, QuestionReviewRepository reviewRepository,
			QualityService quality, QuestionReviewer reviewer, SetMetricsService metrics) {
		this.metrics = metrics;
		this.questionRepository = questionRepository;
		this.setService = setService;
		this.voteRepository = voteRepository;
		this.reviewRepository = reviewRepository;
		this.quality = quality;
		this.reviewer = reviewer;
	}

	/** 좋아요/신고. 1인 1표이고 다시 보내면 바뀐다. 자기 문제에는 할 수 없다. */
	@Transactional
	public QualitySummary vote(Long userId, Long questionId, VoteRequest req) {
		Question q = find(questionId);
		QuestionSet set = setService.requireViewable(userId, q.getQuestionSet().getId());
		if (set.isOwner(userId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내가 만든 문제는 평가할 수 없습니다.");
		}
		if (set.getStatus() != SetStatus.PUBLISHED) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다.");
		}
		// 좋아요에는 이유를 받지 않는다
		VoteReason reason = req.vote() == VoteType.DOWN ? req.reason() : null;
		voteRepository.findByQuestionIdAndUserId(questionId, userId)
				.ifPresentOrElse(v -> v.change(req.vote(), reason),
						() -> voteRepository.save(new QuestionVote(q, userId, req.vote(), reason)));
		voteRepository.flush();
		quality.recompute(q);
		metrics.refresh(set);
		return summary(q, userId, false);
	}

	/** AI 검증. 출제자만 실행할 수 있다. */
	@Transactional
	public QualitySummary review(Long userId, Long questionId) {
		Question q = ownedQuestion(userId, questionId);
		runReview(q);
		return summary(q, userId, true);
	}

	/** 세트의 모든 문제를 차례로 검증한다. 한 문제라도 AI 오류가 나면 거기서 멈추고 오류를 돌려준다. */
	@Transactional
	public List<QualitySummary> reviewSet(Long userId, Long setId) {
		QuestionSet set = setService.requireViewable(userId, setId);
		if (!set.isOwner(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "출제자만 할 수 있습니다.");
		}
		return set.getQuestions().stream().map(q -> {
			runReview(q);
			return summary(q, userId, true);
		}).toList();
	}

	@Transactional(readOnly = true)
	public List<ReviewDto> history(Long userId, Long questionId) {
		ownedQuestion(userId, questionId);
		return quality.reviews(questionId).stream().map(ReviewDto::of).toList();
	}

	@Transactional(readOnly = true)
	public QualitySummary get(Long userId, Long questionId) {
		Question q = find(questionId);
		boolean mine = setService.requireViewable(userId, q.getQuestionSet().getId()).isOwner(userId);
		return summary(q, userId, mine);
	}

	private void runReview(Question q) {
		QuestionReviewer.Result result = reviewer.review(q);
		reviewRepository.save(new QuestionReview(q, result.verdict(), String.join("\n", result.issues())));
		reviewRepository.flush();
		quality.recompute(q);
	}

	private QualitySummary summary(Question q, Long userId, boolean ownerView) {
		QualityService.Stats s = quality.statsFor(q, userId);
		return new QualitySummary(q.getId(), ownerView ? q.getQualityStatus() : null, s.up(), s.down(), s.myVote(),
				ownerView && s.latestReview() != null ? ReviewDto.of(s.latestReview()) : null);
	}

	private Question find(Long id) {
		return questionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."));
	}

	private Question ownedQuestion(Long userId, Long id) {
		Question q = find(id);
		if (!q.getQuestionSet().isOwner(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "출제자만 할 수 있습니다.");
		}
		return q;
	}
}
