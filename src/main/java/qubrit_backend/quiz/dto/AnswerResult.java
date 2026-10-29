package qubrit_backend.quiz.dto;

public record AnswerResult(boolean correct, Long correctChoiceId, String correctAnswer, String explanation,
		String aiFeedback) {
}
