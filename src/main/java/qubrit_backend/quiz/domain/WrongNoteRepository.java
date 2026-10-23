package qubrit_backend.quiz.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WrongNoteRepository extends JpaRepository<WrongNote, Long> {

	Optional<WrongNote> findByUserIdAndQuestionId(Long userId, Long questionId);

	@Query("""
			select w from WrongNote w
			where w.userId = :userId and w.resolved = :resolved
			  and (:subject = '' or w.question.questionSet.subject.name = :subject)
			order by w.lastWrongAt desc""")
	Page<WrongNote> findList(@Param("userId") Long userId, @Param("subject") String subject,
			@Param("resolved") boolean resolved, Pageable pageable);

	long countByUserIdAndResolved(Long userId, boolean resolved);

	List<WrongNote> findByUserIdAndResolvedFalseOrderByLastWrongAtDesc(Long userId, Pageable pageable);

	// 복습 후보: 많이 틀린 것, 최근에 틀린 것 순
	@Query("""
			select w from WrongNote w
			where w.userId = :userId and w.resolved = false
			  and (:subject = '' or w.question.questionSet.subject.name = :subject)
			  and w.question.qualityStatus <> qubrit_backend.quiz.domain.QualityStatus.FAIL
			order by w.wrongCount desc, w.lastWrongAt desc""")
	List<WrongNote> findRetryCandidates(@Param("userId") Long userId, @Param("subject") String subject);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from WrongNote w where w.question.id = :questionId")
	void deleteByQuestionId(@Param("questionId") Long questionId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from WrongNote w where w.question.id in (select q.id from Question q where q.questionSet.id = :setId)")
	void deleteBySetId(@Param("setId") Long setId);
}
