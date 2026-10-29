package qubrit_backend.classroom.domain;

import java.time.Instant;

import jakarta.persistence.*;

/** 학급 과제 (마감일이 있는 숙제) */
@Entity
@Table(name = "classroom_assignments")
public class ClassroomAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(name = "due_at", nullable = false)
	private Instant dueAt;

	protected ClassroomAssignment() {
	}

	public ClassroomAssignment(Long classroomId, String title, Instant dueAt) {
		this.classroomId = classroomId;
		this.title = title;
		this.dueAt = dueAt;
	}

	public Long getClassroomId() { return classroomId; }
	public String getTitle() { return title; }
	public Instant getDueAt() { return dueAt; }
}
