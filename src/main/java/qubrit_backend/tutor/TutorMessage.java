package qubrit_backend.tutor;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

/** Gemini가 만든 과외쌤 응답을 하루 단위로 저장해 두는 캐시 (호출 비용/지연 절감) */
@Entity
@Table(name = "tutor_messages", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "kind", "message_date" }))
public class TutorMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TutorKind kind;

	@Column(name = "message_date", nullable = false)
	private LocalDate messageDate;

	// JSON 문자열
	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected TutorMessage() {
	}

	public TutorMessage(Long userId, TutorKind kind, LocalDate messageDate, String content) {
		this.userId = userId;
		this.kind = kind;
		this.messageDate = messageDate;
		this.content = content;
	}

	public String getContent() { return content; }
}
