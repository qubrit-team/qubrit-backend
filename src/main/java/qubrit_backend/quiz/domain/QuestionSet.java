package qubrit_backend.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import qubrit_backend.subject.Subject;

@Entity
@Table(name = "question_sets")
public class QuestionSet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id")
	private Subject subject;

	@Column(nullable = false)
	private String title;

	@Column(length = 500)
	private String description;

	@Column(name = "share_code", nullable = false, unique = true, length = 8)
	private String shareCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Visibility visibility;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SetSource source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SetStatus status;

	// AI 출제에 쓴 자료 (없으면 null)
	@Column(name = "material_id")
	private Long materialId;

	// 오답 노트 복습용 개인 세트 (목록에 나오지 않는다). enum에 값을 추가하면 기존 DB의 check 제약에 걸려서 별도 컬럼으로 둔다
	@Column(name = "review_set", nullable = false, columnDefinition = "boolean not null default false")
	private boolean reviewSet = false;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "questionSet", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC")
	private List<Question> questions = new ArrayList<>();

	protected QuestionSet() {
	}

	public QuestionSet(Long ownerId, Subject subject, String title, String description, String shareCode,
			Visibility visibility, SetSource source, SetStatus status) {
		this.ownerId = ownerId;
		this.subject = subject;
		this.title = title;
		this.description = description;
		this.shareCode = shareCode;
		this.visibility = visibility;
		this.source = source;
		this.status = status;
	}

	public void update(Subject subject, String title, String description, Visibility visibility) {
		this.subject = subject;
		this.title = title;
		this.description = description;
		this.visibility = visibility;
	}

	public void addQuestion(Question q) {
		q.setPosition(questions.size() + 1);
		questions.add(q);
	}

	public void removeQuestion(Question q) {
		questions.remove(q);
		for (int i = 0; i < questions.size(); i++) {
			questions.get(i).setPosition(i + 1);
		}
	}

	public void linkMaterial(Long materialId) { this.materialId = materialId; }

	public void markReviewSet() { this.reviewSet = true; }

	public void publish() { this.status = SetStatus.PUBLISHED; }

	/** 풀이자에게 보여줄 문제: 품질 검증에서 FAIL 난 문제는 뺀다. */
	public List<Question> visibleQuestions() {
		return questions.stream().filter(q -> q.getQualityStatus() != QualityStatus.FAIL).toList();
	}

	public boolean isOwner(Long userId) { return ownerId.equals(userId); }

	public Long getId() { return id; }
	public Long getOwnerId() { return ownerId; }
	public Subject getSubject() { return subject; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public String getShareCode() { return shareCode; }
	public Visibility getVisibility() { return visibility; }
	public SetSource getSource() { return source; }
	public SetStatus getStatus() { return status; }
	public boolean isReviewSet() { return reviewSet; }
	public Long getMaterialId() { return materialId; }
	public Instant getCreatedAt() { return createdAt; }
	public List<Question> getQuestions() { return questions; }
}
