package qubrit_backend.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionReviewRepository extends JpaRepository<QuestionReview, Long> {

	Optional<QuestionReview> findFirstByQuestionIdOrderByReviewedAtDescIdDesc(Long questionId);

	List<QuestionReview> findByQuestionIdOrderByReviewedAtDesc(Long questionId);

	// 오래된 것 → 최신 순. 호출한 쪽에서 문제별 마지막 값을 쓴다
	@Query("select r from QuestionReview r where r.question.questionSet.id = :setId order by r.reviewedAt asc, r.id asc")
	List<QuestionReview> findBySet(@Param("setId") Long setId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from QuestionReview r where r.question.id = :questionId")
	void deleteByQuestionId(@Param("questionId") Long questionId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from QuestionReview r where r.question.questionSet.id = :setId")
	void deleteBySetId(@Param("setId") Long setId);
}
