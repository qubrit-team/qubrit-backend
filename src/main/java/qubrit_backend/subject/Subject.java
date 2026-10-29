package qubrit_backend.subject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjects")
public class Subject {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected Subject() {
	}

	public Subject(String name, int sortOrder) {
		this.name = name;
		this.sortOrder = sortOrder;
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public int getSortOrder() { return sortOrder; }
}
