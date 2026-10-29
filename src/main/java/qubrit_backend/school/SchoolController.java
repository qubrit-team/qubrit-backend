package qubrit_backend.school;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학교", description = "학교 이름 검색")
@RestController
@RequestMapping("/api/schools")
public class SchoolController {

	private static final int LIMIT = 8;

	private final SchoolRepository repository;

	public SchoolController(SchoolRepository repository) {
		this.repository = repository;
	}

	@Operation(summary = "학교 이름 검색", description = "이름에 검색어가 들어간 학교를 최대 8개 돌려줍니다. 검색어가 비어 있으면 빈 배열입니다.")
	@ApiResponse(responseCode = "200", description = "검색 성공")
	@GetMapping
	public List<SchoolDto> search(@RequestParam(defaultValue = "") String query) {
		String q = query.replaceAll("\\s+", "");
		if (q.isEmpty()) {
			return List.of();
		}
		return repository.findByNameContainingIgnoreCaseOrderByNameAsc(q, PageRequest.of(0, LIMIT)).stream()
				.map(s -> new SchoolDto(s.getId(), s.getName(), s.getRegion()))
				.toList();
	}
}
