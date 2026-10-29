package qubrit_backend.quiz.domain;

import java.time.Instant;

import jakarta.persistence.*;
import qubrit_backend.ai.Difficulty;

/** 문제 세트의 난이도/예상 시간/풀이 수/평균 정답률/신뢰도. 풀이·평가·문제 변경 때마다 다시 계산해 저장한다. */
@Entity
@Table(name = "set_metrics")
public class SetMetrics {

	@Id
	@Column(name = "set_id")
	private Long setId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Difficulty difficulty;

	// 예상 소요 시간(분)
	@Column(name = "estimated_minutes", nullable = false)
	private int estimatedMinutes;

	@Column(name = "solve_count", nullable = false)
	private long solveCount;

	// 평균 정답률(%). 완료된 풀이가 없으면 null
	@Column(name = "avg_accuracy")
	private Integer avgAccuracy;

	// 신뢰도(%): 👍 ÷ 전체 평가. 평가가 없으면 null
	@Column(name = "trust_score")
	private Integer trustScore;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected SetMetrics() {
	}

	public SetMetrics(Long setId) {
		this.setId = setId;
	}

	public void update(Difficulty difficulty, int estimatedMinutes, long solveCount, Integer avgAccuracy,
			Integer trustScore) {
		this.difficulty = difficulty;
		this.estimatedMinutes = estimatedMinutes;
		this.solveCount = solveCount;
		this.avgAccuracy = avgAccuracy;
		this.trustScore = trustScore;
		this.updatedAt = Instant.now();
	}

	public Long getSetId() { return setId; }
	public Difficulty getDifficulty() { return difficulty; }
	public int getEstimatedMinutes() { return estimatedMinutes; }
	public long getSolveCount() { return solveCount; }
	public Integer getAvgAccuracy() { return avgAccuracy; }
	public Integer getTrustScore() { return trustScore; }
	public Instant getUpdatedAt() { return updatedAt; }
}
