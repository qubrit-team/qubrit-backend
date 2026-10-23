package qubrit_backend.review;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record ReviewDto(ReviewVerdict verdict, List<String> issues, Instant reviewedAt) {

	public static ReviewDto of(QuestionReview r) {
		List<String> issues = r.getIssues() == null || r.getIssues().isBlank() ? List.of()
				: Arrays.asList(r.getIssues().split("\n"));
		return new ReviewDto(r.getVerdict(), issues, r.getReviewedAt());
	}
}
