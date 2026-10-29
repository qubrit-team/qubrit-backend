package qubrit_backend.quiz.dto;

import java.time.Instant;
import java.util.List;

import qubrit_backend.quiz.domain.AttemptStatus;

public record AttemptDto(Long id, Long setId, String setTitle, String subject, AttemptStatus status,
		int total, int answeredCount, int score, Instant startedAt, Instant finishedAt,
		List<AttemptAnswerDto> answers) {

	// 이미 제출한 문제에 한해 정답/해설을 함께 내려준다
	public record AttemptAnswerDto(Long questionId, Long selectedChoiceId, String answerText, boolean correct,
			Long correctChoiceId, String correctAnswer, String explanation, String aiFeedback) {
	}
}
