package qubrit_backend.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClassroomSummary(Long id, String name, String subject, int memberCount, boolean owner,
		@Schema(description = "참여 코드 (방장에게만 내려간다)") String joinCode,
		@Schema(description = "최근 7일 안에 공유된 기출 수") int newSetCount,
		@Schema(description = "가장 가까운 과제 마감까지 남은 일수 (진행 중인 과제가 없으면 null)") Integer assignmentDday) {
}
