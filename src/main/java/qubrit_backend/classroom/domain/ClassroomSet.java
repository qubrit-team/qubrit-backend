package qubrit_backend.classroom.domain;

import java.time.Instant;

import jakarta.persistence.*;

/** 학급에 공유된 기출(문제 세트) */
@Entity
@Table(name = "classroom_sets", uniqueConstraints = @UniqueConstraint(columnNames = { "classroom_id", "set_id" }))
public class ClassroomSet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(name = "set_id", nullable = false)
	private Long setId;

	@Column(name = "shared_at", nullable = false)
	private Instant sharedAt;

	protected ClassroomSet() {
	}

	public ClassroomSet(Long classroomId, Long setId, Instant sharedAt) {
		this.classroomId = classroomId;
		this.setId = setId;
		this.sharedAt = sharedAt;
	}

	public Long getClassroomId() { return classroomId; }
	public Long getSetId() { return setId; }
	public Instant getSharedAt() { return sharedAt; }
}
