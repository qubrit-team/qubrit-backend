package qubrit_backend.quiz.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

	Optional<Attempt> findFirstByUserIdAndQuestionSetIdAndStatus(Long userId, Long setId, AttemptStatus status);

	List<Attempt> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

	List<Attempt> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, AttemptStatus status, Pageable pageable);

	@Query("select distinct t.questionSet.id from Attempt t where t.userId = :userId")
	List<Long> findAttemptedSetIds(@Param("userId") Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Attempt t where t.questionSet.id = :setId")
	void deleteBySetId(@Param("setId") Long setId);

	/** 세트의 완료된 풀이 집계 (풀이 수, 맞힌 문제 합, 출제된 문제 합) */
	interface SetTotals {
		long getCount();

		long getScore();

		long getTotal();
	}

	@Query("""
			select count(t) as count, coalesce(sum(t.score), 0) as score, coalesce(sum(t.total), 0) as total
			from Attempt t
			where t.questionSet.id = :setId and t.status = qubrit_backend.quiz.domain.AttemptStatus.DONE""")
	SetTotals totalsForSet(@Param("setId") Long setId);
}
