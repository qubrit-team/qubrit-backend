package qubrit_backend.tutor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "오늘의 과외쌤 추천")
public record TutorToday(
		@Schema(description = "홈 화면 큰 문구 (이름은 앞에 붙여서 사용)", example = "오늘은 국어 3문제 어때요?") String headline,
		@Schema(description = "응원 메시지") String cheer,
		@Schema(description = "추천 과목 (없으면 null)") String focusSubject,
		@Schema(description = "추천 문제 수") int focusCount,
		@Schema(description = "AI가 만든 문구면 true, 규칙 기반 대체 문구면 false") boolean ai) {
}
