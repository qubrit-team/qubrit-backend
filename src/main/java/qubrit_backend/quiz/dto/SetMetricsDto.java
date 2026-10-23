package qubrit_backend.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import qubrit_backend.ai.Difficulty;

@Schema(description = "세트 지표 (DB에 저장된 값, 풀이/평가/문제 변경 시 갱신)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SetMetricsDto(
		@Schema(description = "난이도. 평균 정답률 80% 이상 EASY, 50% 이상 MEDIUM, 그 미만 HARD (풀이가 없으면 MEDIUM)", example = "MEDIUM")
		Difficulty difficulty,
		@Schema(description = "예상 소요 시간(분). 객관식 1분, 주관식 2분, 지문이 있으면 +1분", example = "12")
		int estimatedMinutes,
		@Schema(description = "완료된 풀이 수", example = "42")
		long solveCount,
		@Schema(description = "평균 정답률(%). 완료된 풀이가 없으면 생략", example = "73")
		Integer avgAccuracy,
		@Schema(description = "신뢰도(%) = 👍 ÷ 전체 평가. 평가가 없으면 생략", example = "88")
		Integer trustScore) {
}
