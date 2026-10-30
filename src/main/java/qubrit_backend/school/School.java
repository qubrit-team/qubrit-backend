package qubrit_backend.school;

import jakarta.persistence.*;

@Entity
@Table(name = "schools")
public class School {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	// 시/도 + 시/군/구 (예: 서울 구로구)
	@Column(length = 50)
	private String region;

	protected School() {
	}

	public School(String name, String region) {
		this.name = name;
		this.region = region;
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public String getRegion() { return region; }
}
