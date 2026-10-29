package qubrit_backend.recommend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.quiz.QuestionSetService;
import qubrit_backend.quiz.domain.AttemptAnswerRepository;
import qubrit_backend.quiz.domain.AttemptRepository;
import qubrit_backend.quiz.domain.QuestionSet;
import qubrit_backend.quiz.domain.QuestionSetRepository;
import qubrit_backend.review.QuestionVoteRepository;
import qubrit_backend.review.VoteType;

/**
 * 규칙 기반 추천. 후보는 공개된 세트 중 내가 만들지도, 풀어 보지도 않은 것이다.
 * 점수 = 내 오답률이 높은 과목일수록 +(최대 3) / 관심 과목 +1 / 다른 사용자 평가(👍-👎) ±0.9 / 최신일수록 +(최대 0.3)
 */
@Service
public class RecommendationService {

	private static final int CANDIDATE_LIMIT = 200;
	private static final int MIN_ANSWERS_FOR_STATS = 3; // 이보다 적게 풀었으면 정답률을 믿지 않는다

	private record SubjectStat(int right, int wrong) {
		int total() { return right + wrong; }
		double wrongRate() { return total() == 0 ? 0.5 : (double) wrong / total(); }
	}

	private final QuestionSetRepository setRepository;
	private final AttemptRepository attemptRepository;
	private final AttemptAnswerRepository answerRepository;
	private final QuestionVoteRepository voteRepository;
	private final UserRepository userRepository;
	private final QuestionSetService setService;

	public RecommendationService(QuestionSetRepository setRepository, AttemptRepository attemptRepository,
			AttemptAnswerRepository answerRepository, QuestionVoteRepository voteRepository,
			UserRepository userRepository, QuestionSetService setService) {
		this.setRepository = setRepository;
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.voteRepository = voteRepository;
		this.userRepository = userRepository;
		this.setService = setService;
	}

	@Transactional(readOnly = true)
	public List<RecommendationDto> recommend(Long userId, String subject, int size) {
		String filter = subject == null ? "" : subject.trim();
		List<QuestionSet> candidates = setRepository
				.findPublic(filter, "", PageRequest.of(0, CANDIDATE_LIMIT)).getContent(); // 최신순

		Set<Long> attempted = Set.copyOf(attemptRepository.findAttemptedSetIds(userId));
		Map<String, SubjectStat> stats = subjectStats(userId);
		Set<String> interests = Set.copyOf(userRepository.findById(userId).map(u -> u.getSubjects()).orElse(List.of()));
		Map<Long, int[]> votes = setVotes();

		record Scored(QuestionSet set, double score, String reason) {
		}
		List<Scored> scored = new ArrayList<>();
		for (int rank = 0; rank < candidates.size(); rank++) {
			QuestionSet s = candidates.get(rank);
			if (s.isOwner(userId) || attempted.contains(s.getId()) || s.visibleQuestions().isEmpty()) {
				continue;
			}
			int[] v = votes.getOrDefault(s.getId(), new int[2]);
			int net = v[0] - v[1];
			if (net <= -3) {
				continue; // 평가가 나쁜 세트는 추천하지 않는다
			}
			String name = s.getSubject().getName();
			SubjectStat st = stats.getOrDefault(name, new SubjectStat(0, 0));
			boolean enough = st.total() >= MIN_ANSWERS_FOR_STATS;
			double weakness = enough ? st.wrongRate() : 0.5;
			boolean interest = interests.contains(name);

			double score = weakness * 3 + (interest ? 1 : 0) + Math.max(-3, Math.min(3, net)) * 0.3
					+ (1 - (double) rank / candidates.size()) * 0.3;

			String reason;
			if (enough && st.wrongRate() >= 0.4) {
				reason = name + " 정답률이 " + Math.round((1 - st.wrongRate()) * 100) + "%예요. 복습해 보세요";
			} else if (interest) {
				reason = "관심 과목(" + name + ")이에요";
			} else if (v[0] >= 3 && net > 0) {
				reason = "다른 친구들에게 평가가 좋은 세트예요";
			} else {
				reason = "새로 올라온 세트예요";
			}
			scored.add(new Scored(s, score, reason));
		}
		scored.sort(Comparator.comparingDouble(Scored::score).reversed());
		List<Scored> top = scored.stream().limit(Math.min(Math.max(size, 1), 30)).toList();

		List<QuestionSet> sets = top.stream().map(Scored::set).toList();
		var summaries = setService.summaries(sets, userId);
		List<RecommendationDto> result = new ArrayList<>();
		for (int i = 0; i < top.size(); i++) {
			result.add(new RecommendationDto(summaries.get(i), top.get(i).reason()));
		}
		return result;
	}

	/** 같은 학급 친구들이 많이 푼 공개 세트. 이미 푼 세트도 포함한다(내가 만든 세트만 제외). */
	@Transactional(readOnly = true)
	public List<ClassmatePopularDto> classmatePopular(Long userId, int size) {
		int limit = Math.min(Math.max(size, 1), 30);
		List<Object[]> rows = attemptRepository.classmatePopularSets(userId, PageRequest.of(0, limit));
		Map<Long, QuestionSet> byId = new HashMap<>();
		setRepository.findAllById(rows.stream().map(r -> (Long) r[0]).toList()).forEach(s -> byId.put(s.getId(), s));

		List<QuestionSet> sets = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		for (Object[] row : rows) {
			QuestionSet s = byId.get((Long) row[0]);
			if (s == null || s.visibleQuestions().isEmpty()) {
				continue;
			}
			sets.add(s);
			counts.add(((Long) row[1]).intValue());
		}
		var summaries = setService.summaries(sets, userId);
		List<ClassmatePopularDto> result = new ArrayList<>();
		for (int i = 0; i < sets.size(); i++) {
			result.add(new ClassmatePopularDto(summaries.get(i), counts.get(i)));
		}
		return result;
	}

	private Map<String, SubjectStat> subjectStats(Long userId) {
		Map<String, int[]> raw = new HashMap<>();
		for (Object[] row : answerRepository.subjectStats(userId)) {
			int[] c = raw.computeIfAbsent((String) row[0], k -> new int[2]);
			c[(Boolean) row[1] ? 0 : 1] = ((Long) row[2]).intValue();
		}
		Map<String, SubjectStat> result = new HashMap<>();
		raw.forEach((k, c) -> result.put(k, new SubjectStat(c[0], c[1])));
		return result;
	}

	// 세트 ID → [좋아요 수, 신고 수]
	private Map<Long, int[]> setVotes() {
		Map<Long, int[]> result = new HashMap<>();
		for (Object[] row : voteRepository.countAllBySet()) {
			result.computeIfAbsent((Long) row[0], k -> new int[2])[row[1] == VoteType.UP ? 0 : 1] = ((Long) row[2]).intValue();
		}
		return result;
	}
}
