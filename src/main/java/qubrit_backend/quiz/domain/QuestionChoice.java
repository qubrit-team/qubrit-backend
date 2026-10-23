package qubrit_backend.quiz.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "question_choices")
public class QuestionChoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false, length = 500)
	private String content;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	protected QuestionChoice() {
	}

	public QuestionChoice(Question question, String content, boolean correct) {
		this.question = question;
		this.content = content;
		this.correct = correct;
	}

	void setPosition(int position) { this.position = position; }

	public Long getId() { return id; }
	public int getPosition() { return position; }
	public String getContent() { return content; }
	public boolean isCorrect() { return correct; }
}
