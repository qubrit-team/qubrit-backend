package qubrit_backend.quiz.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_answers", uniqueConstraints = @UniqueConstraint(columnNames = { "attempt_id", "question_id" }))
public class AttemptAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id")
	private Attempt attempt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	@Column(name = "selected_choice_id")
	private Long selectedChoiceId;

	@Column(name = "answer_text", length = 500)
	private String answerText;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	@Column(name = "ai_feedback", length = 2000)
	private String aiFeedback;

	@Column(name = "answered_at", nullable = false)
	private Instant answeredAt = Instant.now();

	protected AttemptAnswer() {
	}

	public AttemptAnswer(Attempt attempt, Question question, Long selectedChoiceId, String answerText, boolean correct,
			String aiFeedback) {
		this.aiFeedback = aiFeedback;
		this.attempt = attempt;
		this.question = question;
		this.selectedChoiceId = selectedChoiceId;
		this.answerText = answerText;
		this.correct = correct;
	}

	public Long getId() { return id; }
	public Question getQuestion() { return question; }
	public Long getSelectedChoiceId() { return selectedChoiceId; }
	public String getAnswerText() { return answerText; }
	public boolean isCorrect() { return correct; }
	public String getAiFeedback() { return aiFeedback; }
}
