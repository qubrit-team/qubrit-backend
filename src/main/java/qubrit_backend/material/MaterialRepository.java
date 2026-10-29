package qubrit_backend.material;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Long> {

	// subject/q가 빈 문자열이면 조건 없음
	@Query("""
			select m from Material m
			where (:subject = '' or m.subject.name = :subject)
			  and lower(m.title) like lower(concat('%', :q, '%'))
			order by m.createdAt desc""")
	Page<Material> search(@Param("subject") String subject, @Param("q") String q, Pageable pageable);
}
