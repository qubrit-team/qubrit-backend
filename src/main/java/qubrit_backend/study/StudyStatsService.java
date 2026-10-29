package qubrit_backend.study;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.auth.domain.User;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.quiz.domain.AttemptAnswerRepository;
import qubrit_backend.quiz.domain.WrongNoteRepository;

@Service
public class StudyStatsService {

	public static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int STREAK_LOOKBACK_DAYS = 400;

	private final AttemptAnswerRepository answerRepository;
	private final WrongNoteRepository wrongNoteRepository;
	private final UserRepository userRepository;

	public StudyStatsService(AttemptAnswerRepository answerRepository, WrongNoteRepository wrongNoteRepository,
			UserRepository userRepository) {
		this.answerRepository = answerRepository;
		this.wrongNoteRepository = wrongNoteRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public StudyStats compute(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		LocalDate today = LocalDate.now(KST);

		List<Instant> answered = answerRepository.answeredSince(userId,
				today.minusDays(STREAK_LOOKBACK_DAYS).atStartOfDay(KST).toInstant());
		Set<LocalDate> days = answered.stream().map(t -> t.atZone(KST).toLocalDate()).collect(Collectors.toSet());

		LocalDate weekStart = today.with(DayOfWeek.MONDAY);
		int weeklyDone = (int) answered.stream()
				.filter(t -> !t.atZone(KST).toLocalDate().isBefore(weekStart)).count();

		List<String> recentWrong = wrongNoteRepository
				.findByUserIdAndResolvedFalseOrderByLastWrongAtDesc(userId, PageRequest.of(0, 8)).stream()
				.map(w -> w.getQuestion().getContent()).toList();

		return new StudyStats(subjectStats(userId), streak(days, today), weeklyDone, user.getWeeklyGoal(),
				(int) wrongNoteRepository.countByUserIdAndResolved(userId, false), recentWrong);
	}

	/** 연속 학습 일수. 오늘 아직 안 풀었어도 어제까지의 연속은 유지된다. */
	static int streak(Set<LocalDate> days, LocalDate today) {
		LocalDate d = days.contains(today) ? today : today.minusDays(1);
		int count = 0;
		while (days.contains(d)) {
			count++;
			d = d.minusDays(1);
		}
		return count;
	}

	private List<SubjectStatDto> subjectStats(Long userId) {
		Map<String, int[]> raw = new HashMap<>();
		for (Object[] row : answerRepository.subjectStats(userId)) {
			raw.computeIfAbsent((String) row[0], k -> new int[2])[(Boolean) row[1] ? 0 : 1] = ((Long) row[2]).intValue();
		}
		return raw.entrySet().stream()
				.map(e -> new SubjectStatDto(e.getKey(), e.getValue()[0], e.getValue()[1],
						(int) Math.round(100.0 * e.getValue()[0] / (e.getValue()[0] + e.getValue()[1]))))
				.sorted(Comparator.comparing(SubjectStatDto::subject))
				.toList();
	}
}
