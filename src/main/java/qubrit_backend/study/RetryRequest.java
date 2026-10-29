package qubrit_backend.study;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "오답 복습 옵션. 모두 생략 가능 (과목: 오답이 가장 많은 과목, 문제 수: 10)")
public record RetryRequest(String subject, @Min(1) @Max(30) Integer size) {
}
