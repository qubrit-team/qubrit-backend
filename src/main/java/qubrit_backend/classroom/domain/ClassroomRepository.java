package qubrit_backend.classroom.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

	Optional<Classroom> findByJoinCode(String joinCode);

	boolean existsByJoinCode(String joinCode);

	/** 내가 속한 학급 (최근에 만든 순) */
	@Query("select c from Classroom c where c.id in "
			+ "(select m.classroom.id from ClassroomMember m where m.userId = :userId) order by c.createdAt desc")
	List<Classroom> findJoinedBy(@Param("userId") Long userId);
}
