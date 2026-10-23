package qubrit_backend.quiz.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "wrong_notes", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "question_id" }))
public class WrongNote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	@Column(nullable = false)
	private boolean resolved;

	@Column(name = "wrong_count", nullable = false)
	private int wrongCount;

	@Column(name = "last_wrong_at", nullable = false)
	private Instant lastWrongAt = Instant.now();

	protected WrongNote() {
	}

	public WrongNote(Long userId, Question question) {
		this.userId = userId;
		this.question = question;
	}

	public void markWrong() {
		this.resolved = false;
		this.wrongCount++;
		this.lastWrongAt = Instant.now();
	}

	public void markResolved() { this.resolved = true; }

	public Long getId() { return id; }
	public Question getQuestion() { return question; }
	public boolean isResolved() { return resolved; }
	public int getWrongCount() { return wrongCount; }
	public java.time.Instant getLastWrongAt() { return lastWrongAt; }
}
