package qubrit_backend.review;

import java.time.Instant;

import jakarta.persistence.*;
import qubrit_backend.quiz.domain.Question;

@Entity
@Table(name = "question_votes", uniqueConstraints = @UniqueConstraint(columnNames = { "question_id", "user_id" }))
public class QuestionVote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VoteType vote;

	@Enumerated(EnumType.STRING)
	private VoteReason reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected QuestionVote() {
	}

	public QuestionVote(Question question, Long userId, VoteType vote, VoteReason reason) {
		this.question = question;
		this.userId = userId;
		this.vote = vote;
		this.reason = reason;
	}

	public void change(VoteType vote, VoteReason reason) {
		this.vote = vote;
		this.reason = reason;
	}

	public Question getQuestion() { return question; }
	public VoteType getVote() { return vote; }
	public VoteReason getReason() { return reason; }
}
