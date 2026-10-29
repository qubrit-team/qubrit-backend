package qubrit_backend.review;

import java.time.Instant;

import jakarta.persistence.*;
import qubrit_backend.quiz.domain.Question;

/** AI가 문제를 검증한 결과. 문제를 다시 검증할 때마다 한 줄씩 쌓고, 가장 최근 것을 쓴다. */
@Entity
@Table(name = "question_reviews")
public class QuestionReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReviewVerdict verdict;

	// 지적 사항을 줄바꿈으로 이어 붙여 저장
	@Column(columnDefinition = "text")
	private String issues;

	@Column(name = "reviewed_at", nullable = false)
	private Instant reviewedAt = Instant.now();

	protected QuestionReview() {
	}

	public QuestionReview(Question question, ReviewVerdict verdict, String issues) {
		this.question = question;
		this.verdict = verdict;
		this.issues = issues;
	}

	public Question getQuestion() { return question; }
	public ReviewVerdict getVerdict() { return verdict; }
	public String getIssues() { return issues; }
	public Instant getReviewedAt() { return reviewedAt; }
}
