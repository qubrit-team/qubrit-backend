package qubrit_backend.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionVoteRepository extends JpaRepository<QuestionVote, Long> {

	Optional<QuestionVote> findByQuestionIdAndUserId(Long questionId, Long userId);

	// [questionId, vote, count]
	@Query("select v.question.id, v.vote, count(v) from QuestionVote v where v.question.questionSet.id = :setId group by v.question.id, v.vote")
	List<Object[]> countBySet(@Param("setId") Long setId);

	@Query("select v from QuestionVote v where v.question.questionSet.id = :setId and v.userId = :userId")
	List<QuestionVote> findMineBySet(@Param("setId") Long setId, @Param("userId") Long userId);

	// [setId, vote, count] — 추천 점수용
	@Query("select v.question.questionSet.id, v.vote, count(v) from QuestionVote v group by v.question.questionSet.id, v.vote")
	List<Object[]> countAllBySet();

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from QuestionVote v where v.question.id = :questionId")
	void deleteByQuestionId(@Param("questionId") Long questionId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from QuestionVote v where v.question.questionSet.id = :setId")
	void deleteBySetId(@Param("setId") Long setId);
}
