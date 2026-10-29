package qubrit_backend.school;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {

	// 공백을 뺀 이름에 검색어가 들어 있는 학교 (이름 순)
	List<School> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
