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

	/**
	 * 같은 학급 친구들이 많이 푼 공개 세트: [세트 id, 푼 친구 수].
	 * 친구 = 나와 한 학급 이상에 함께 속한 다른 사용자, 많이 푼 순(같으면 최근 세트 우선).
	 */
	@Query("""
			select t.questionSet.id, count(distinct t.userId)
			from Attempt t
			where t.userId <> :userId
			  and t.questionSet.status = qubrit_backend.quiz.domain.SetStatus.PUBLISHED
			  and t.questionSet.visibility = qubrit_backend.quiz.domain.Visibility.PUBLIC
			  and t.questionSet.ownerId <> :userId
			  and t.userId in (
			      select m2.userId from qubrit_backend.classroom.domain.ClassroomMember m2
			      where m2.classroom.id in (
			          select m.classroom.id from qubrit_backend.classroom.domain.ClassroomMember m where m.userId = :userId))
			group by t.questionSet.id
			order by count(distinct t.userId) desc, t.questionSet.id desc""")
	List<Object[]> classmatePopularSets(@Param("userId") Long userId, Pageable pageable);
}
