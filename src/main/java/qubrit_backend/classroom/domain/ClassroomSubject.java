package qubrit_backend.classroom.domain;

import jakarta.persistence.*;

/** 학급 만들기에서 고를 수 있는 과목(분야). 사용자가 직접 추가할 수 있다. */
@Entity
@Table(name = "classroom_subjects")
public class ClassroomSubject {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected ClassroomSubject() {
	}

	public ClassroomSubject(String name, int sortOrder) {
		this.name = name;
		this.sortOrder = sortOrder;
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public int getSortOrder() { return sortOrder; }
}
