package qubrit_backend.study;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "주간 목표 변경 (한 주에 풀 문제 수)")
public record GoalRequest(@Min(1) @Max(200) int weeklyGoal) {
}
