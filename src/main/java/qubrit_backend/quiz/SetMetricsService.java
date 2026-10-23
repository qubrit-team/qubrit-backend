package qubrit_backend.quiz;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qubrit_backend.ai.Difficulty;
import qubrit_backend.quiz.domain.*;
import qubrit_backend.quiz.dto.SetMetricsDto;
import qubrit_backend.review.QuestionVoteRepository;
import qubrit_backend.review.VoteType;

/** 세트 지표(난이도/예상 시간/풀이 수/평균 정답률/신뢰도)를 계산해 set_metrics 테이블에 저장한다. */
@Service
public class SetMetricsService {

	private final SetMetricsRepository metricsRepository;
	private final QuestionSetRepository setRepository;
	private final AttemptRepository attemptRepository;
	private final QuestionVoteRepository voteRepository;

	public SetMetricsService(SetMetricsRepository metricsRepository, QuestionSetRepository setRepository,
			AttemptRepository attemptRepository, QuestionVoteRepository voteRepository) {
		this.metricsRepository = metricsRepository;
		this.setRepository = setRepository;
		this.attemptRepository = attemptRepository;
		this.voteRepository = voteRepository;
	}

	@Transactional
	public SetMetrics refresh(Long setId) {
		return setRepository.findById(setId).map(this::refresh).orElse(null);
	}

	@Transactional
	public SetMetrics refresh(QuestionSet set) {
		var totals = attemptRepository.totalsForSet(set.getId());
		Integer accuracy = totals.getTotal() == 0 ? null
				: (int) Math.round(totals.getScore() * 100.0 / totals.getTotal());

		long up = 0;
		long down = 0;
		for (Object[] row : voteRepository.countBySet(set.getId())) {
			if (row[1] == VoteType.UP) {
				up += (Long) row[2];
			} else {
				down += (Long) row[2];
			}
		}
		Integer trust = up + down == 0 ? null : (int) Math.round(up * 100.0 / (up + down));

		SetMetrics m = metricsRepository.findById(set.getId()).orElseGet(() -> new SetMetrics(set.getId()));
		m.update(difficultyOf(accuracy), estimateMinutes(set.getQuestions()), totals.getCount(), accuracy, trust);
		return metricsRepository.save(m);
	}

	/** 저장된 지표. 아직 없으면 계산해서 저장한다. */
	@Transactional
	public SetMetricsDto view(QuestionSet set) {
		SetMetrics m = metricsRepository.findById(set.getId()).orElseGet(() -> refresh(set));
		return new SetMetricsDto(m.getDifficulty(), m.getEstimatedMinutes(), m.getSolveCount(), m.getAvgAccuracy(),
				m.getTrustScore());
	}

	/** 모든 세트의 지표를 다시 계산한다 (기동 시 보정용). */
	@Transactional
	public void refreshAll() {
		setRepository.findAll().forEach(this::refresh);
	}

	private static Difficulty difficultyOf(Integer accuracy) {
		if (accuracy == null) {
			return Difficulty.MEDIUM;
		}
		return accuracy >= 80 ? Difficulty.EASY : accuracy >= 50 ? Difficulty.MEDIUM : Difficulty.HARD;
	}

	private static int estimateMinutes(List<Question> questions) {
		int minutes = 0;
		for (Question q : questions) {
			minutes += q.getType() == QuestionType.SHORT ? 2 : 1;
			if (q.getPassage() != null && !q.getPassage().isBlank()) {
				minutes += 1;
			}
		}
		return Math.max(minutes, 1);
	}
}
