package qubrit_backend.classroom.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom_members", uniqueConstraints = @UniqueConstraint(columnNames = { "classroom_id", "user_id" }))
public class ClassroomMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id")
	private Classroom classroom;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt = Instant.now();

	protected ClassroomMember() {
	}

	public ClassroomMember(Classroom classroom, Long userId) {
		this.classroom = classroom;
		this.userId = userId;
	}

	public Classroom getClassroom() { return classroom; }
	public Long getUserId() { return userId; }
	public Instant getJoinedAt() { return joinedAt; }
}
