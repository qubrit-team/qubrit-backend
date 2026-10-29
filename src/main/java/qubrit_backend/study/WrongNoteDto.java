package qubrit_backend.study;

import java.time.Instant;

import qubrit_backend.quiz.domain.QuestionType;

public record WrongNoteDto(Long id, Long questionId, Long setId, String setTitle, String subject,
		QuestionType type, String content, int wrongCount, boolean resolved, Instant lastWrongAt) {
}
