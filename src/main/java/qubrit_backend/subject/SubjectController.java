package qubrit_backend.subject;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "과목", description = "과목 목록")
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

	private final SubjectRepository subjectRepository;

	public SubjectController(SubjectRepository subjectRepository) {
		this.subjectRepository = subjectRepository;
	}

	@Operation(summary = "과목 목록", description = "과목 이름을 정렬 순서대로 문자열 배열로 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public List<String> list() {
		return subjectRepository.findAllByOrderBySortOrderAsc().stream().map(Subject::getName).toList();
	}
}
