package qubrit_backend.auth.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	// BCrypt로 암호화된 값
	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String nickname;

	// 관심 과목 (user_subjects 테이블에 별도 저장)
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_subjects", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "subject")
	private List<String> subjects = new ArrayList<>();

	// 주간 학습 목표(풀이한 문제 수), 기본값: 15
	@Column(name = "weekly_goal", nullable = false, columnDefinition = "integer not null default 15")
	private int weeklyGoal = 15;

	protected User() {
	}

	public User(String email, String password, String nickname, List<String> subjects) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.subjects = new ArrayList<>(subjects);
	}

	public int getWeeklyGoal() { return weeklyGoal; }
	public void changeWeeklyGoal(int weeklyGoal) { this.weeklyGoal = weeklyGoal; }

	public Long getId() { return id; }
	public String getEmail() { return email; }
	public String getPassword() { return password; }
	public String getNickname() { return nickname; }
	public List<String> getSubjects() { return subjects; }
}
