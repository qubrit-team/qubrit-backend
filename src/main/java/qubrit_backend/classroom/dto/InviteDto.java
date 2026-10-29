package qubrit_backend.classroom.dto;

public record InviteDto(Long id, String inviterNickname, Long classroomId, String classroomName, int memberCount,
		int setCount) {
}
