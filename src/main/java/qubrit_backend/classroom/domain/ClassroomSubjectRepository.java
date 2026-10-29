package qubrit_backend.classroom.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomSubjectRepository extends JpaRepository<ClassroomSubject, Long> {

	List<ClassroomSubject> findAllByOrderBySortOrderAscIdAsc();

	boolean existsByName(String name);
}
