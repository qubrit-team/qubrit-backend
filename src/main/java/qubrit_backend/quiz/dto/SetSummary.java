package qubrit_backend.quiz.dto;

import java.time.Instant;

import qubrit_backend.quiz.domain.SetSource;
import qubrit_backend.quiz.domain.SetStatus;
import qubrit_backend.quiz.domain.Visibility;

public record SetSummary(Long id, String title, String description, String subject, String ownerNickname,
		int questionCount, Visibility visibility, SetStatus status, SetSource source, String shareCode,
		boolean mine, Instant createdAt) {
}
