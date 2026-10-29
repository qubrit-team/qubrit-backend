package qubrit_backend.classroom.domain;

import java.time.Instant;

import jakarta.persistence.*;
import qubrit_backend.subject.Subject;

@Entity
@Table(name = "classrooms")
public class Classroom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	// 학급 분야(과목 목록과 무관한 자유 입력: 파이썬, 알고리즘, 자격증 ...)
	@Column(nullable = false, length = 30)
	private String subject;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "join_code", nullable = false, unique = true, length = 8)
	private String joinCode;

	// 학급 만들기 화면에서 받는 부가 설정 (기존 학급은 null)
	@Column(length = 50)
	private String school;

	private Integer grade;

	@Column(name = "class_no")
	private Integer classNo;

	// 선택한 과목들을 쉼표로 이어 붙여 저장
	@Column(length = 200)
	private String subjects;

	// APPROVAL(방장 승인) | CODE(코드만 있으면 바로 참여)
	@Column(name = "join_mode", length = 10)
	private String joinMode;

	@Column(name = "member_upload_allowed", nullable = false, columnDefinition = "boolean default true")
	private boolean memberUploadAllowed = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Classroom() {
	}

	public Classroom(Long ownerId, String subject, String name, String joinCode) {
		this.ownerId = ownerId;
		this.subject = subject;
		this.name = name;
		this.joinCode = joinCode;
	}

	public void applySettings(String school, Integer grade, Integer classNo, String subjects, String joinMode,
			boolean memberUploadAllowed) {
		this.school = school;
		this.grade = grade;
		this.classNo = classNo;
		this.subjects = subjects;
		this.joinMode = joinMode;
		this.memberUploadAllowed = memberUploadAllowed;
	}

	public boolean isOwner(Long userId) { return ownerId.equals(userId); }

	public Long getId() { return id; }
	public Long getOwnerId() { return ownerId; }
	public String getSubject() { return subject; }
	public String getName() { return name; }
	public String getJoinCode() { return joinCode; }
	public String getSchool() { return school; }
	public Integer getGrade() { return grade; }
	public Integer getClassNo() { return classNo; }
	public String getSubjects() { return subjects; }
	public String getJoinMode() { return joinMode; }
	public boolean isMemberUploadAllowed() { return memberUploadAllowed; }
	public Instant getCreatedAt() { return createdAt; }
}
