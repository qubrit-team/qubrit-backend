package qubrit_backend.classroom.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomAssignmentRepository extends JpaRepository<ClassroomAssignment, Long> {

	/** 학급별로 아직 지나지 않은 가장 가까운 마감 시각: [학급 id, 마감 시각] */
	@Query("select a.classroomId, min(a.dueAt) from ClassroomAssignment a where a.classroomId in :ids and a.dueAt >= :now group by a.classroomId")
	List<Object[]> nextDueByClassrooms(@Param("ids") List<Long> ids, @Param("now") Instant now);
}
