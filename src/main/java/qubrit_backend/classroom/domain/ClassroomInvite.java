package qubrit_backend.classroom.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom_invites")
public class ClassroomInvite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(name = "inviter_id", nullable = false)
	private Long inviterId;

	@Column(name = "invitee_id", nullable = false)
	private Long inviteeId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InviteStatus status = InviteStatus.PENDING;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected ClassroomInvite() {
	}

	public ClassroomInvite(Long classroomId, Long inviterId, Long inviteeId) {
		this.classroomId = classroomId;
		this.inviterId = inviterId;
		this.inviteeId = inviteeId;
	}

	public void respond(InviteStatus status) { this.status = status; }

	public Long getId() { return id; }
	public Long getClassroomId() { return classroomId; }
	public Long getInviterId() { return inviterId; }
	public Long getInviteeId() { return inviteeId; }
	public InviteStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
}
