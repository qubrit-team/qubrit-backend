package qubrit_backend.quiz.dto;

import java.util.List;

public record SetDetail(SetSummary info, List<QuestionDto> questions) {
}
