package qubrit_backend.subject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

	List<Subject> findAllByOrderBySortOrderAsc();

	Optional<Subject> findByName(String name);
}
