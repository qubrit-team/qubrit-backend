package qubrit_backend.quiz.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

	List<AttemptAnswer> findByAttemptId(Long attemptId);

	Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

	// [과목 이름, 정답 여부, 개수]
	@Query("select t.questionSet.subject.name, a.correct, count(a) from AttemptAnswer a join a.attempt t where t.userId = :userId group by t.questionSet.subject.name, a.correct")
	List<Object[]> subjectStats(@Param("userId") Long userId);

	@Query("select a.answeredAt from AttemptAnswer a join a.attempt t where t.userId = :userId and a.answeredAt >= :since")
	List<java.time.Instant> answeredSince(@Param("userId") Long userId, @Param("since") java.time.Instant since);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from AttemptAnswer a where a.question.id = :questionId")
	void deleteByQuestionId(@Param("questionId") Long questionId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from AttemptAnswer a where a.attempt.id in (select t.id from Attempt t where t.questionSet.id = :setId)")
	void deleteBySetId(@Param("setId") Long setId);
}
