package qubrit_backend.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "set_id")
	private QuestionSet questionSet;

	@Column(nullable = false)
	private int position;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuestionType type;

	@Column(nullable = false, length = 2000)
	private String content;

	// 지문(긴 글). 없으면 null
	@Column(length = 4000)
	private String passage;

	@Column(length = 2000)
	private String explanation;

	// 주관식 정답
	@Column(name = "answer_text", length = 500)
	private String answerText;

	// 오답 노트 복습용으로 복사된 문제의 원본 (원본이 지워져도 남도록 FK는 걸지 않는다)
	@Column(name = "origin_question_id")
	private Long originQuestionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "quality_status", nullable = false)
	private QualityStatus qualityStatus = QualityStatus.PENDING;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC")
	private List<QuestionChoice> choices = new ArrayList<>();

	protected Question() {
	}

	public Question(QuestionSet questionSet, QuestionType type, String content, String passage, String explanation,
			String answerText) {
		this.passage = passage;
		this.questionSet = questionSet;
		this.type = type;
		this.content = content;
		this.explanation = explanation;
		this.answerText = answerText;
	}

	public void update(QuestionType type, String content, String passage, String explanation, String answerText) {
		this.passage = passage;
		this.type = type;
		this.content = content;
		this.explanation = explanation;
		this.answerText = answerText;
		this.qualityStatus = QualityStatus.PENDING; // 내용이 바뀌면 다시 검증 대상
	}

	public void addChoice(QuestionChoice c) {
		c.setPosition(choices.size() + 1);
		choices.add(c);
	}

	public void copiedFrom(Long originQuestionId) { this.originQuestionId = originQuestionId; }

	public void applyQuality(QualityStatus status) { this.qualityStatus = status; }

	public void clearChoices() { choices.clear(); }

	void setPosition(int position) { this.position = position; }

	public Long getId() { return id; }
	public Long getOriginQuestionId() { return originQuestionId; }
	public QuestionSet getQuestionSet() { return questionSet; }
	public int getPosition() { return position; }
	public QuestionType getType() { return type; }
	public String getContent() { return content; }
	public String getPassage() { return passage; }
	public String getExplanation() { return explanation; }
	public String getAnswerText() { return answerText; }
	public QualityStatus getQualityStatus() { return qualityStatus; }
	public List<QuestionChoice> getChoices() { return choices; }
}
