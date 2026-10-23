package qubrit_backend.quiz.dto;

import java.time.Instant;

import qubrit_backend.quiz.domain.AttemptStatus;

public record AttemptSummary(Long id, Long setId, String setTitle, String subject, AttemptStatus status,
		int total, int answeredCount, int score, Instant startedAt) {
}
