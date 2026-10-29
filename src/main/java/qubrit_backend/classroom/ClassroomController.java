package qubrit_backend.classroom;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.classroom.dto.ClassroomRequest;
import qubrit_backend.classroom.dto.ClassroomSummary;
import qubrit_backend.classroom.dto.InviteDto;
import qubrit_backend.classroom.dto.JoinRequest;

@Tag(name = "학급", description = "학급 만들기, 참여 코드로 참여, 내 학급 목록")
@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

	private final ClassroomService service;

	public ClassroomController(ClassroomService service) {
		this.service = service;
	}

	@Operation(summary = "내 학급 목록", description = "내가 만들었거나 참여한 학급을 최근에 만든 순으로 돌려줍니다. 참여 코드는 방장에게만 내려갑니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (참여한 학급이 없으면 빈 배열)")
	@GetMapping
	public List<ClassroomSummary> listMine(@AuthenticationPrincipal Long userId) {
		return service.listMine(userId);
	}

	@Operation(summary = "학급 만들기", description = "선생님/학생 구분 없이 로그인한 누구나 만들 수 있습니다. 만든 사람이 방장이 되고, 8자리 참여 코드가 발급됩니다.")
	@ApiResponse(responseCode = "201", description = "생성 성공 (joinCode 포함)")
	@ApiResponse(responseCode = "400", description = "이름/분야가 비었거나 너무 김")
	@ApiResponse(responseCode = "401", description = "로그인 필요")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ClassroomSummary create(@AuthenticationPrincipal Long userId, @Valid @RequestBody ClassroomRequest req) {
		return service.create(userId, req);
	}

	@Operation(summary = "참여 코드로 학급 참여", description = "대소문자 구분 없이 참여 코드를 받습니다.")
	@ApiResponse(responseCode = "200", description = "참여 성공")
	@ApiResponse(responseCode = "404", description = "참여 코드를 찾을 수 없음")
	@ApiResponse(responseCode = "409", description = "이미 참여 중인 학급")
	@PostMapping("/join")
	public ClassroomSummary join(@AuthenticationPrincipal Long userId, @Valid @RequestBody JoinRequest req) {
		return service.join(userId, req.code());
	}

	@Operation(summary = "받은 초대 목록", description = "아직 응답하지 않은 초대를 최근 순으로 돌려줍니다.")
	@GetMapping("/invites")
	public List<InviteDto> listInvites(@AuthenticationPrincipal Long userId) {
		return service.listInvites(userId);
	}

	@Operation(summary = "초대 수락", description = "수락하면 해당 학급의 멤버가 됩니다.")
	@PostMapping("/invites/{id}/accept")
	public ClassroomSummary acceptInvite(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.acceptInvite(userId, id);
	}

	@Operation(summary = "초대 거절")
	@PostMapping("/invites/{id}/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void declineInvite(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		service.declineInvite(userId, id);
	}
}
