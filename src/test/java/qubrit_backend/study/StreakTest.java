package qubrit_backend.study;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StreakTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

	@Test
	void noActivityIsZero() {
		assertEquals(0, StudyStatsService.streak(Set.of(), TODAY));
	}

	@Test
	void countsConsecutiveDaysIncludingToday() {
		assertEquals(3, StudyStatsService.streak(Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
	}

	@Test
	void streakSurvivesUntilTodayEndsWhenYesterdayWasStudied() {
		assertEquals(2, StudyStatsService.streak(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
	}

	@Test
	void gapBreaksStreak() {
		assertEquals(1, StudyStatsService.streak(Set.of(TODAY, TODAY.minusDays(2)), TODAY));
		assertEquals(0, StudyStatsService.streak(Set.of(TODAY.minusDays(2), TODAY.minusDays(3)), TODAY));
	}
}
