package qubrit_backend.classroom.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomInviteRepository extends JpaRepository<ClassroomInvite, Long> {

	List<ClassroomInvite> findByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, InviteStatus status);
}
