package qubrit_backend.material;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import qubrit_backend.quiz.dto.PageResponse;

import java.nio.charset.StandardCharsets;

@Tag(name = "학습 자료실", description = "자료 업로드/조회/다운로드")
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

	private final MaterialService service;

	public MaterialController(MaterialService service) {
		this.service = service;
	}

	@Operation(summary = "자료 업로드", description = "multipart/form-data. pdf, txt, md, png, jpg 지원(최대 20MB).")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public MaterialDto upload(@AuthenticationPrincipal Long userId, @RequestParam MultipartFile file,
			@RequestParam(required = false) String title, @RequestParam String subject) {
		return service.upload(userId, file, title, subject);
	}

	@Operation(summary = "자료 목록", description = "subject, q(제목 검색)로 거를 수 있고 최신순입니다.")
	@GetMapping
	public PageResponse<MaterialDto> list(@AuthenticationPrincipal Long userId,
			@RequestParam(required = false) String subject, @RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.list(userId, subject, q, page, size);
	}

	@Operation(summary = "자료 상세")
	@GetMapping("/{id}")
	public MaterialDto get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return service.get(userId, id);
	}

	@Operation(summary = "원본 다운로드")
	@GetMapping("/{id}/download")
	public ResponseEntity<byte[]> download(@PathVariable Long id) {
		Material m = service.find(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(m.getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(m.getFileName(), StandardCharsets.UTF_8).build().toString())
				.body(service.read(m));
	}

	@Operation(summary = "자료 삭제", description = "올린 사람만 가능합니다.")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		service.delete(userId, id);
	}
}
