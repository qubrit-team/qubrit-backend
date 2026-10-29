package qubrit_backend.quiz.dto;

import java.util.List;

import qubrit_backend.review.ReviewDto;
import qubrit_backend.review.VoteType;

import com.fasterxml.jackson.annotation.JsonInclude;
import qubrit_backend.quiz.domain.QualityStatus;
import qubrit_backend.quiz.domain.QuestionType;

// explanation/answerText/choices[].correct/qualityStatus/aiReview는 출제자에게만 내려간다 (풀이자에게는 null → 생략)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionDto(Long id, int position, QuestionType type, String content, String passage,
		String explanation,
		String answerText, QualityStatus qualityStatus, List<ChoiceDto> choices, int upVotes, int downVotes,
		VoteType myVote, ReviewDto aiReview) {
}
