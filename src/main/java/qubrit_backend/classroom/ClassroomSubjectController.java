package qubrit_backend.classroom;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.classroom.domain.ClassroomSubject;
import qubrit_backend.classroom.domain.ClassroomSubjectRepository;
import qubrit_backend.classroom.dto.SubjectRequest;

@Tag(name = "학급 과목", description = "학급 만들기에서 고르는 과목 목록과 직접 추가")
@RestController
@RequestMapping("/api/classroom-subjects")
public class ClassroomSubjectController {

	private final ClassroomSubjectRepository repository;

	public ClassroomSubjectController(ClassroomSubjectRepository repository) {
		this.repository = repository;
	}

	@Operation(summary = "학급 과목 목록", description = "정렬 순서대로 과목 이름 배열을 돌려줍니다.")
	@GetMapping
	public List<String> list() {
		return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(ClassroomSubject::getName).toList();
	}

	@Operation(summary = "학급 과목 추가", description = "이미 있는 이름이면 새로 만들지 않고 그대로 돌려줍니다 (대소문자 구분).")
	@ApiResponse(responseCode = "201", description = "추가(또는 기존) 과목 이름")
	@ApiResponse(responseCode = "400", description = "이름이 비었거나 너무 김")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public String add(@Valid @RequestBody SubjectRequest req) {
		String name = req.name().trim();
		if (name.isEmpty()) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "과목 이름을 입력해 주세요.");
		}
		if (!repository.existsByName(name)) {
			repository.save(new ClassroomSubject(name, (int) repository.count()));
		}
		return name;
	}
}
