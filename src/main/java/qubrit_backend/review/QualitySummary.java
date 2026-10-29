package qubrit_backend.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import qubrit_backend.quiz.domain.QualityStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QualitySummary(Long questionId, QualityStatus qualityStatus, int upVotes, int downVotes,
		VoteType myVote, ReviewDto aiReview) {
}
