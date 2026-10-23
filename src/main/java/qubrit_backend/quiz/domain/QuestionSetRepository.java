package qubrit_backend.quiz.domain;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {

	Optional<QuestionSet> findByShareCode(String shareCode);

	boolean existsByShareCode(String shareCode);

	// subject/q는 빈 문자열이면 조건 없음 (PostgreSQL에서 null 파라미터 타입 추론 오류를 피하기 위함)
	@Query("""
			select s from QuestionSet s
			where s.ownerId = :userId
			  and s.reviewSet = false
			  and (:subject = '' or s.subject.name = :subject)
			  and lower(s.title) like lower(concat('%', :q, '%'))
			order by s.createdAt desc""")
	Page<QuestionSet> findMine(@Param("userId") Long userId, @Param("subject") String subject,
			@Param("q") String q, Pageable pageable);

	@Query("""
			select s from QuestionSet s
			where s.status = qubrit_backend.quiz.domain.SetStatus.PUBLISHED
			  and s.visibility = qubrit_backend.quiz.domain.Visibility.PUBLIC
			  and (:subject = '' or s.subject.name = :subject)
			  and lower(s.title) like lower(concat('%', :q, '%'))
			order by s.createdAt desc""")
	Page<QuestionSet> findPublic(@Param("subject") String subject, @Param("q") String q, Pageable pageable);

	// 인기순: 풀이 시작 횟수가 많은 세트부터 (같으면 최신순)
	@Query("""
			select s from QuestionSet s
			where s.status = qubrit_backend.quiz.domain.SetStatus.PUBLISHED
			  and s.visibility = qubrit_backend.quiz.domain.Visibility.PUBLIC
			  and (:subject = '' or s.subject.name = :subject)
			  and lower(s.title) like lower(concat('%', :q, '%'))
			order by (select count(t) from Attempt t where t.questionSet = s) desc, s.createdAt desc""")
	Page<QuestionSet> findPublicByPopularity(@Param("subject") String subject, @Param("q") String q, Pageable pageable);

	// 신뢰도순: 문제 평가(👍 +1, 👎 -1) 합계가 높은 세트부터 (같으면 최신순)
	@Query("""
			select s from QuestionSet s
			where s.status = qubrit_backend.quiz.domain.SetStatus.PUBLISHED
			  and s.visibility = qubrit_backend.quiz.domain.Visibility.PUBLIC
			  and (:subject = '' or s.subject.name = :subject)
			  and lower(s.title) like lower(concat('%', :q, '%'))
			order by (select coalesce(sum(case when v.vote = qubrit_backend.review.VoteType.UP then 1 else -1 end), 0)
			          from QuestionVote v where v.question.questionSet = s) desc, s.createdAt desc""")
	Page<QuestionSet> findPublicByTrust(@Param("subject") String subject, @Param("q") String q, Pageable pageable);
}
