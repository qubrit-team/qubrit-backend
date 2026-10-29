package qubrit_backend.review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import qubrit_backend.quiz.domain.QualityStatus;
import qubrit_backend.quiz.domain.Question;

/**
 * 문제 품질 상태 계산. AI 검증 결과와 사용자 평가(👍/👎)를 합쳐 Question.qualityStatus에 저장한다.
 * 규칙:
 *  - AI가 FAIL이거나, 👎가 5개 이상이고 👍의 2배를 넘으면 FAIL (풀이 화면에서 숨김)
 *  - AI가 WARN이거나, 👎가 3개 이상이고 👍보다 많으면 WARN
 *  - AI가 PASS이거나, 👍가 3개 이상이고 👎보다 많으면 PASS
 *  - 그 외 PENDING
 */
@Service
public class QualityService {

	public record Stats(int up, int down, VoteType myVote, QuestionReview latestReview) {
		public static Stats empty() {
			return new Stats(0, 0, null, null);
		}
	}

	private final QuestionVoteRepository voteRepository;
	private final QuestionReviewRepository reviewRepository;

	public QualityService(QuestionVoteRepository voteRepository, QuestionReviewRepository reviewRepository) {
		this.voteRepository = voteRepository;
		this.reviewRepository = reviewRepository;
	}

	/** 세트에 속한 문제들의 통계를 한 번에 구한다 (문제 ID → 통계). */
	public Map<Long, Stats> statsForSet(Long setId, Long userId) {
		Map<Long, int[]> counts = new HashMap<>();
		for (Object[] row : voteRepository.countBySet(setId)) {
			int[] c = counts.computeIfAbsent((Long) row[0], k -> new int[2]);
			c[row[1] == VoteType.UP ? 0 : 1] = ((Long) row[2]).intValue();
		}
		Map<Long, VoteType> mine = new HashMap<>();
		voteRepository.findMineBySet(setId, userId).forEach(v -> mine.put(v.getQuestion().getId(), v.getVote()));
		Map<Long, QuestionReview> latest = new HashMap<>();
		for (QuestionReview r : reviewRepository.findBySet(setId)) {
			latest.put(r.getQuestion().getId(), r);
		}
		Map<Long, Stats> result = new HashMap<>();
		counts.keySet().forEach(id -> result.put(id, null));
		mine.keySet().forEach(id -> result.put(id, null));
		latest.keySet().forEach(id -> result.put(id, null));
		result.replaceAll((id, v) -> {
			int[] c = counts.getOrDefault(id, new int[2]);
			return new Stats(c[0], c[1], mine.get(id), latest.get(id));
		});
		return result;
	}

	public Stats statsFor(Question q, Long userId) {
		return statsForSet(q.getQuestionSet().getId(), userId).getOrDefault(q.getId(), Stats.empty());
	}

	/** 투표/검증이 바뀐 뒤에 호출해 qualityStatus를 다시 계산한다. */
	public void recompute(Question q) {
		int up = 0;
		int down = 0;
		for (Object[] row : voteRepository.countBySet(q.getQuestionSet().getId())) {
			if (row[0].equals(q.getId())) {
				if (row[1] == VoteType.UP) {
					up = ((Long) row[2]).intValue();
				} else {
					down = ((Long) row[2]).intValue();
				}
			}
		}
		ReviewVerdict ai = reviewRepository.findFirstByQuestionIdOrderByReviewedAtDescIdDesc(q.getId())
				.map(QuestionReview::getVerdict).orElse(null);
		q.applyQuality(decide(ai, up, down));
	}

	static QualityStatus decide(ReviewVerdict ai, int up, int down) {
		if (ai == ReviewVerdict.FAIL || (down >= 5 && down > up * 2)) {
			return QualityStatus.FAIL;
		}
		if (ai == ReviewVerdict.WARN || (down >= 3 && down > up)) {
			return QualityStatus.WARN;
		}
		if (ai == ReviewVerdict.PASS || (up >= 3 && up > down)) {
			return QualityStatus.PASS;
		}
		return QualityStatus.PENDING;
	}

	public void deleteForQuestion(Long questionId) {
		voteRepository.deleteByQuestionId(questionId);
		reviewRepository.deleteByQuestionId(questionId);
	}

	public void deleteForSet(Long setId) {
		voteRepository.deleteBySetId(setId);
		reviewRepository.deleteBySetId(setId);
	}

	public List<QuestionReview> reviews(Long questionId) {
		return reviewRepository.findByQuestionIdOrderByReviewedAtDesc(questionId);
	}
}
