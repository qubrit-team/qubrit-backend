package qubrit_backend.study;

import java.util.List;

import qubrit_backend.quiz.dto.AttemptSummary;
import qubrit_backend.tutor.TutorToday;

public record HomeResponse(String nickname, TutorToday tutor, GoalProgress weeklyGoal, int streak,
		int wrongNoteCount, List<AttemptSummary> inProgress) {

	public record GoalProgress(int done, int goal) {
	}
}
