package qubrit_backend.recommend;

import qubrit_backend.quiz.dto.SetSummary;

public record RecommendationDto(SetSummary set, String reason) {
}
