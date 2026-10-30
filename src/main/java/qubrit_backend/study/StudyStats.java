package qubrit_backend.study;

import java.util.List;

/** 과외쌤/홈이 함께 쓰는 학습 현황 */
public record StudyStats(List<SubjectStatDto> subjects, int streak, int weeklyDone, int weeklyGoal,
		int unresolvedWrong, List<String> recentWrong) {

	public int totalAnswered() {
		return subjects.stream().mapToInt(s -> s.right() + s.wrong()).sum();
	}
}
