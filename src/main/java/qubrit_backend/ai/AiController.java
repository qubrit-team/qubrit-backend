package qubrit_backend.ai;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import qubrit_backend.material.Material;
import qubrit_backend.material.MaterialService;
import qubrit_backend.quiz.QuestionSetService;
import qubrit_backend.quiz.domain.QuestionType;
import qubrit_backend.quiz.dto.QuestionRequest;
import qubrit_backend.quiz.dto.SetDetail;

@Tag(name = "AI 출제", description = "자료/주제 기반 문제 생성 (결과는 초안 세트로 저장됨)")
@RestController
@RequestMapping("/api")
public class AiController {

	private static final int DEFAULT_COUNT = 5;

	private final QuestionGenerator generator;
	private final QuestionSetService setService;
	private final MaterialService materialService;

	public AiController(QuestionGenerator generator, QuestionSetService setService, MaterialService materialService) {
		this.generator = generator;
		this.setService = setService;
		this.materialService = materialService;
	}

	@Operation(summary = "자료 기반 AI 출제",
			description = "자료에서 문제를 만들어 DRAFT 세트로 저장합니다. 검토/수정 후 /api/sets/{id}/publish로 발행하세요. Gemini 호출 때문에 수십 초 걸릴 수 있습니다.")
	@PostMapping("/materials/{id}/generate")
	@ResponseStatus(HttpStatus.CREATED)
	public SetDetail fromMaterial(@AuthenticationPrincipal Long userId, @PathVariable Long id,
			@Valid @RequestBody(required = false) GenerateRequest req) {
		Material m = materialService.find(id);
		String text = m.getExtractedText();
		GeminiClient.Attachment attachment = text != null ? null
				: new GeminiClient.Attachment(m.getContentType(), materialService.read(m));
		GenerateRequest r = req == null ? new GenerateRequest(null, null, null) : req;
		List<QuestionRequest> questions = generator.generate(m.getSubject().getName(), text, attachment, null,
				count(r.count()), types(r.types()), difficulty(r.difficulty()));
		return setService.createDraft(userId, m.getSubject().getName(), "[AI] " + m.getTitle(), m.getId(), questions);
	}

	@Operation(summary = "주제 기반 AI 출제", description = "자료 없이 과목과 주제만으로 문제를 만들어 DRAFT 세트로 저장합니다.")
	@PostMapping("/ai/generate")
	@ResponseStatus(HttpStatus.CREATED)
	public SetDetail fromTopic(@AuthenticationPrincipal Long userId, @Valid @RequestBody TopicGenerateRequest req) {
		List<QuestionRequest> questions = generator.generate(req.subject().trim(), null, null, req.topic().trim(),
				count(req.count()), types(req.types()), difficulty(req.difficulty()));
		return setService.createDraft(userId, req.subject(), "[AI] " + req.topic().trim(), null, questions);
	}

	private static int count(Integer c) { return c == null ? DEFAULT_COUNT : c; }

	private static List<QuestionType> types(List<QuestionType> t) {
		return t == null || t.isEmpty() ? List.of(QuestionType.MCQ, QuestionType.SHORT) : t;
	}

	private static Difficulty difficulty(Difficulty d) { return d == null ? Difficulty.MEDIUM : d; }
}
