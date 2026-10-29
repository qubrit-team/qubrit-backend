package qubrit_backend.tutor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import qubrit_backend.study.SubjectStatDto;

@Schema(description = "학습 분석")
public record TutorAnalysis(
		@Schema(description = "과목별 정답/오답 수와 정답률 (DB에서 계산)") List<SubjectStatDto> subjects,
		int streak,
		@Schema(description = "한두 문장 총평") String summary,
		@Schema(description = "자주 틀리는 부분") List<String> weaknesses,
		@Schema(description = "공부 조언") String advice,
		@Schema(description = "AI가 만든 분석이면 true, 규칙 기반 대체면 false") boolean ai) {
}
