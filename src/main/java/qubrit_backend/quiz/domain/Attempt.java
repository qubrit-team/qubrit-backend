package qubrit_backend.quiz.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "attempts")
public class Attempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "set_id")
	private QuestionSet questionSet;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AttemptStatus status = AttemptStatus.IN_PROGRESS;

	private int score;
	private int total;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt = Instant.now();

	@Column(name = "finished_at")
	private Instant finishedAt;

	protected Attempt() {
	}

	public Attempt(Long userId, QuestionSet questionSet) {
		this.userId = userId;
		this.questionSet = questionSet;
		this.total = questionSet.visibleQuestions().size();
	}

	public void finish(int score) {
		this.score = score;
		this.status = AttemptStatus.DONE;
		this.finishedAt = Instant.now();
	}

	public Long getId() { return id; }
	public Long getUserId() { return userId; }
	public QuestionSet getQuestionSet() { return questionSet; }
	public AttemptStatus getStatus() { return status; }
	public int getScore() { return score; }
	public int getTotal() { return total; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getFinishedAt() { return finishedAt; }
}
